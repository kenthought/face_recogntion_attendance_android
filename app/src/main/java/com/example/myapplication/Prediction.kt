package com.example.myapplication

import android.graphics.Rect

data class Prediction( var bbox : Rect, var label : String ,  var maskLabel : String = "" )