package swaix.validationlibrary

import android.view.View

/**
 * Created by swaix on 11/02/2018.
 */
interface ValidItemListener {
    fun onValidItem() {}

    fun onNotValidItem() {}

    fun onValidField(fieldView: View, correctValue: Any) {}

    fun onNotValidField(fieldView: View, correctValue: Any) {}

}