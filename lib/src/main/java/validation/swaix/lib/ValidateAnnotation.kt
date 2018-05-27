package swaix.validationlibrary

import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import com.google.firebase.database.Exclude
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.joda.time.DateTime
import validation.swaix.lib.R
import java.lang.reflect.Field
import java.util.regex.Pattern

/**
 *
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Required

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EmailType

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class PasswordType(val pattern: String = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@!#\$%^&+=.]{8,}\$")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class NumberBetween(val min: Double, val max: Double)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Bind(val id: Int = -1)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ErrorMessage(val message: Int = -1)

open class ValidItem {

    var delay = 600

    @set:Exclude
    @get:Exclude
    var listener: ValidItemListener? = null

    @get:Exclude
    val invalidFields: MutableList<Field> = mutableListOf()

    fun isValid(field: Field) {
        invalidFields.remove(field)
        val annotation = field.declaredAnnotations
        if (annotation.isNotEmpty()) {
            try {
                field.isAccessible = true
                val item = field.get(this)

                annotation.forEach {
                    when (it) {
                        is Required -> when (item) {
                            null -> addIfError(true, field)
                            is String -> addIfError(item.toString().isBlank(), field)
                            is Double -> addIfError(item == Double.NaN, field)
                        }
                        is EmailType -> addIfError(item != null && !Patterns.EMAIL_ADDRESS.matcher(item.toString()).matches(), field)
                        is PasswordType -> {
                            addIfError(item != null && !validateStringAndPattern(it.pattern, item.toString()), field)
                        }
                        is NumberBetween -> {
                            val actualNumberValue = item.toString().toDoubleOrNull() ?: Double.NaN
                            addIfError(item != null && actualNumberValue !in (it.min..it.max), field)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addIfError(predicate: Boolean, field: Field) {
        if (predicate && field !in invalidFields)
            invalidFields.add(field)
    }

    fun isValid(): Boolean {
        invalidFields.clear()
        javaClass.declaredFields.forEach {
            isValid(it)
        }

        val isValid = invalidFields.isEmpty()

        if (isValid)
            listener?.onValidItem()
        else
            listener?.onNotValidItem()

        return isValid
    }

    fun bind(fields: Map<Field, View>, unknownFormat: ((View, Field, Int) -> Unit)? = null) {
        fields.forEach({
            val field = it.key
            val view = it.value
            val error = (field.declaredAnnotations.find {
                it is ErrorMessage
            } as? ErrorMessage)?.message ?: R.string.label_error
            bindViews(view, field, error, unknownFormat)
        })
    }

    fun bind(container: View, unknownFormat: ((View, Field, Int) -> Unit)? = null) {
        val b = this.javaClass.declaredFields.mapNotNull {
            val annotations = it.declaredAnnotations.filter {
                it is Bind
            }
            if (annotations.isNotEmpty()) {
                Pair(it, annotations.filter { it is Bind }.map { (it as Bind).id }.first())
            } else
                null
        }
        val errors = b.map {
            (it.first.declaredAnnotations.find {
                it is ErrorMessage
            } as? ErrorMessage)?.message ?: R.string.label_error
        }

        (0 until b.size).forEach {
            if (b[it].second != -1) {
                val field = b[it].first
                val view = container.findViewById<View>(b[it].second)
                val error = errors[it]
                bindViews(view, field, error, unknownFormat)
            }
        }
    }

    private fun bindViews(view: View, field: Field, error: Int, unknownFormat: ((View, Field, Int) -> Unit)?) {
        when (view) {
            is CompoundButton -> setSwitchListener(view, field)
            is TextView -> setTextViewWatcher(view, field, error)
            is TextInputLayout -> setTextViewWatcher(view.editText as TextView, field, error)
            else -> unknownFormat?.invoke(view, field, error)
        }
    }

    private fun setSwitchListener(view: CompoundButton, field: Field) {
        field.isAccessible = true
        val item = field.get(this)
        view.isChecked = item.toString().toBoolean()
        view.setOnCheckedChangeListener { _, isChecked ->
            field.isAccessible = true
            field.set(this@ValidItem, isChecked)
            listener?.onValidField(view, isChecked)
        }
    }

    private fun <T : TextView> setTextViewWatcher(view: T, field: Field, error: Int) {
        field.isAccessible = true
        val item = field.get(this)
        if (item != null)
            view.text = item.toString()
        view.addTextChangedListener(object : TextWatcher {
            var lastTypeTime: Long = 0
            override fun afterTextChanged(s: Editable?) {
                view.error = null
                if (invalidFields.contains(field)) {
                    launch(UI) {
                        delay(delay)
                        if (DateTime.now().millis > lastTypeTime + delay) {
                            view.error = view.context.getString(error)
                            listener?.onNotValidField(view, s.toString())
                        }
                    }
                } else {
                    listener?.onValidField(view, s.toString())
                }
                isValid()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastTypeTime = DateTime.now().millis
                view.error = null
                field.isAccessible = true
                var value = s.toString()
                when (field.type) {
                    String::class.java -> {
                        field.set(this@ValidItem, value)
                    }
                    Int::class.java -> {
                        if (value.isBlank())
                            value = "0"
                        field.set(this@ValidItem, value.toInt())
                    }
                    Long::class.java -> {
                        if (value.isBlank())
                            value = "0"
                        field.set(this@ValidItem, value.toLong())
                    }
                    Float::class.java -> {
                        if (value.isBlank())
                            value = "0"
                        field.set(this@ValidItem, value.toFloat())
                    }
                    Double::class.java -> {
                        if (value.isBlank())
                            value = "0"
                        field.set(this@ValidItem, value.toDouble())
                    }
                }
                isValid(field)
            }
        })
    }


    private fun validateStringAndPattern(PATTERN: String, string: String): Boolean {
        val pattern = Pattern.compile(PATTERN)
        val matcher = pattern.matcher(string)
        return matcher.matches()
    }
}