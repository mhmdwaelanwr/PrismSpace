package com.prismspace.container.core;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import com.prismspace.container.PrismSpaceCore;

/**
 * H6: Distribution Layer. Manages policy blob lifecycle and distribution via IPC.
 * Implements coalescing to prevent rapid rebuild overhead.
 */
public class PolicyPublisher {
    private static final String TAG = "PolicyPublisher";
    private static final long COALESCE_DELAY_MS = 300;

    public interface BlobCompiler {
        ByteBuffer compile(ClonePolicySnapshot snapshot);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final BlobCompiler mCompiler;
    private final AtomicReference<PublishedBlob> mCurrentBlob = new AtomicReference<>(null);
    
    private ClonePolicySnapshot mPendingSnapshot = null;
    private final Runnable mPublishRunnable = this::doPublish;

    public PolicyPublisher(BlobCompiler compiler) {
        this.mCompiler = compiler;
    }

    public static class PublishedBlob {
        public final ParcelFileDescriptor pfd;
        public final int size;
        public final long generation;

        public PublishedBlob(ParcelFileDescriptor pfd, int size, long generation) {
            this.pfd = pfd;
            this.size = size;
            this.generation = generation;
        }
    }

    /**
     * Schedules a policy update with coalescing.
     */
    public synchronized void updatePolicy(ClonePolicySnapshot snapshot) {
        mPendingSnapshot = snapshot;
        mHandler.removeCallbacks(mPublishRunnable);
        mHandler.postDelayed(mPublishRunnable, COALESCE_DELAY_MS);
    }

    public synchronized boolean publishNow(ClonePolicySnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        mPendingSnapshot = null;
        try {
            ByteBuffer buffer = mCompiler.compile(snapshot);
            int size = buffer.remaining();

            PublishedBlob newBlob;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                newBlob = createSharedMemoryBlob(buffer, size, snapshot.generation);
            } else {
                newBlob = createFileBackedBlob(buffer, size, snapshot.generation);
            }

            if (newBlob == null) {
                return false;
            }

            PublishedBlob old = mCurrentBlob.getAndSet(newBlob);
            if (old != null && old.pfd != null) {
                try { old.pfd.close(); } catch (IOException ignored) {}
            }
            Log.i(TAG, "Published policy generation " + snapshot.generation + " (size: " + size + ")");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to compile or publish policy", e);
            return false;
        }
    }

    private synchronized void doPublish() {
        if (mPendingSnapshot == null) return;

        ClonePolicySnapshot snapshot = mPendingSnapshot;
        mPendingSnapshot = null;
        publishNow(snapshot);
    }

    private PublishedBlob createSharedMemoryBlob(ByteBuffer data, int size, long gen) throws ErrnoException, IOException {
        SharedMemory shm = SharedMemory.create("prism_policy_" + gen, size);
        try {
            ByteBuffer mapping = shm.mapReadWrite();
            mapping.put(data);
            shm.unmap(mapping);
            shm.setProtect(android.system.OsConstants.PROT_READ);
            
            // Extract FD via reflection
            int fd = getFdFromSharedMemory(shm);
            return new PublishedBlob(ParcelFileDescriptor.fromFd(fd), size, gen);
        } catch (Exception e) {
            shm.close();
            throw e;
        }
    }

    private PublishedBlob createFileBackedBlob(ByteBuffer data, int size, long gen) throws IOException {
        File dir = new File(PrismSpaceCore.getContext().getNoBackupFilesDir(), "prism_policies");
        if (!dir.exists()) dir.mkdirs();
        
        File file = new File(dir, "policy_" + gen + ".bin");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] bytes = new byte[size];
            data.get(bytes);
            fos.write(bytes);
            fos.getFD().sync();
            return new PublishedBlob(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), size, gen);
        }
    }

    @Nullable
    public PublishedBlob getCurrentBlob() {
        return mCurrentBlob.get();
    }

    private int getFdFromSharedMemory(SharedMemory shm) {
        try {
            Field fdField = SharedMemory.class.getDeclaredField("mFileDescriptor");
            fdField.setAccessible(true);
            java.io.FileDescriptor fdObj = (java.io.FileDescriptor) fdField.get(shm);
            Field descriptorField = java.io.FileDescriptor.class.getDeclaredField("descriptor");
            descriptorField.setAccessible(true);
            return (int) descriptorField.get(fdObj);
        } catch (Exception e) {
            return -1;
        }
    }
    
    // Default compiler implementation
    public static class DefaultBlobCompiler implements BlobCompiler {
        @Override
        public ByteBuffer compile(ClonePolicySnapshot snapshot) {
            PolicySnapshotBuilder builder = new PolicySnapshotBuilder();
            for (ClonePolicySnapshot.IoRule rule : snapshot.ioRules) {
                builder.addRule(rule.src, rule.dst, rule.exact, rule.precedence);
            }
            for (ClonePolicySnapshot.BinderRule rule : snapshot.binderRules) {
                builder.addBinderSpoof(rule.originalAppId, rule.spoofAppId);
            }
            return builder.build(snapshot.generation);
        }
    }
}
