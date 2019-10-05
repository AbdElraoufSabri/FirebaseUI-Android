package com.firebase.ui.auth.ui;


import androidx.annotation.*;

/**
 * View (Activity or Fragment, normally) that can respond to progress events.
 */
public interface ProgressView {

    void showProgress(@StringRes int message);

    void hideProgress();

}
