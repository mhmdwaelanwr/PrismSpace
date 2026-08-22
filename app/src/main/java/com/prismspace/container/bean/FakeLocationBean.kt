package com.prismspace.container.bean

import android.graphics.drawable.Drawable
import com.prismspace.container.entity.location.PLocation

data class FakeLocationBean(
    val userID: Int,
    val name: String,
    val icon: Drawable,
    val packageName: String,
    var fakeLocationPattern: Int,
    var fakeLocation: PLocation?
)

data class FakeLocationBeanInstallBean(val userID: Int, val success: Boolean, val msg: String)
