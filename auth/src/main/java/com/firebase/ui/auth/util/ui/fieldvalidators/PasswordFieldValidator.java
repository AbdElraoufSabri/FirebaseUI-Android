/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.util.ui.fieldvalidators;


import com.firebase.ui.auth.R;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.RestrictTo;

public class PasswordFieldValidator extends BaseValidator {
    private int mMinLength;

    public PasswordFieldValidator(TextInputLayout errorContainer, int minLength) {
        super(errorContainer);
        mMinLength = minLength;
        mErrorMessage = mErrorContainer.getResources()
                .getQuantityString(R.plurals.fui_error_weak_password, mMinLength, mMinLength);
    }

    @Override
    protected boolean isValid(CharSequence charSequence) {
        return charSequence.length() >= mMinLength;
    }
}
