package com.jeppeman.mockposable.integrationtests.android

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelizeTest(val dummy: Int) : Parcelable
