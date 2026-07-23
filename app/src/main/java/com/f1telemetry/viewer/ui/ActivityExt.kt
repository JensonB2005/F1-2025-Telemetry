package com.f1telemetry.viewer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walk the ContextWrapper chain to the hosting Activity (for orientation control). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
