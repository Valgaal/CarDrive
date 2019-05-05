package com.example.cardrive

import android.graphics.Bitmap
import android.graphics.Matrix

fun Bitmap.getResizedBitmap(newWidth: Int, newHeight: Int): Bitmap {
    val width = this.width
    val height = this.height
    val scaleWidth = newWidth.toFloat() / width
    val scaleHeight = newHeight.toFloat() / height

    val matrix = Matrix()

    matrix.postScale(scaleWidth, scaleHeight)

    val resizedBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, false)
    this.recycle()
    return resizedBitmap
}