package com.adguard.android.myapplication.root

/** Possible device's root states */
sealed class RootState {

    /** The device is not rooted */
    object NotRooted : RootState()



    /** The device is rooted with given [rootType] */
    class Rooted(val rootType: RootType) : RootState()



    /** Device's root types */
    enum class RootType {
        Magisk, Other
    }
}
