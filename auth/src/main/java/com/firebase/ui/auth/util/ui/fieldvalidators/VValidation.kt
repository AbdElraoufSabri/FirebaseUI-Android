package com.firebase.ui.auth.util.ui.fieldvalidators

import android.text.*
import com.afollestad.vvalidator.field.FieldError
import com.google.android.material.textfield.*

class VValidation {
    companion object {
        fun unsetErrorOfLayoutOnTyping(layout: TextInputLayout, editText: TextInputEditText) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    layout.error = null
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

            })
        }

        fun formatErrorMessages(errors: List<FieldError>, inputLayout: TextInputLayout) {
            val messages = getCombinedMessages(errors, StringBuilder())
            var formatted = splitAndFormatMessages(messages)
            setErrorInView(formatted, inputLayout)
        }

        private fun setErrorInView(tmp: String, editText: TextInputLayout) {
            if (tmp.isNotEmpty())
                editText.error = tmp
        }

        private fun splitAndFormatMessages(messages: String): String {
            var tmp = ""
            val split = messages.split("##")

            split.forEachIndexed { index, s ->
                if (s.isNotEmpty()) {
                    tmp += "-$s"
                    if (index < split.size - 1)
                        tmp += "\n"
                }
            }
            return tmp
        }

        private fun getCombinedMessages(errors: List<FieldError>, messages: StringBuilder): String {
            errors.forEach { messages.append(it.description) }
            return messages.toString()
        }

    }
}