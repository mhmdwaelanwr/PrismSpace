package com.prismspace.container.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Md5Utils {
    private val hexDigits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
        'e', 'f'
    )

    @JvmStatic
    fun md5(input: String?): String? {
        if (input == null) return null
        return try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val inputByteArray = input.toByteArray(Charsets.UTF_8)
            messageDigest.update(inputByteArray)
            val resultByteArray = messageDigest.digest()
            byteArrayToHex(resultByteArray)
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    fun md5(file: File): String? {
        try {
            if (!file.isFile) {
                return null
            }
            val input = FileInputStream(file)
            val result = md5(input)
            input.close()
            return result
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun md5(input: InputStream): String? {
        try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                messageDigest.update(buffer, 0, read)
            }
            input.close()
            return byteArrayToHex(messageDigest.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun byteArrayToHex(byteArray: ByteArray): String {
        val resultCharArray = CharArray(byteArray.size * 2)
        var index = 0
        for (b in byteArray) {
            resultCharArray[index++] = hexDigits[b.toInt() ushr 4 and 0xf]
            resultCharArray[index++] = hexDigits[b.toInt() and 0xf]
        }
        return String(resultCharArray)
    }
}

