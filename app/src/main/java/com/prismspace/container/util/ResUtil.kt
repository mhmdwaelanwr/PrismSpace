package com.prismspace.container.util

import androidx.annotation.StringRes
import com.prismspace.container.app.App


fun getString(@StringRes id:Int,vararg arg:String):String{
    if(arg.isEmpty()){
        return App.getContext().getString(id)
    }
    return App.getContext().getString(id,*arg)
}

