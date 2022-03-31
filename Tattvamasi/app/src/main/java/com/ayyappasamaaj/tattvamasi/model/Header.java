package com.ayyappasamaaj.tattvamasi.model;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

public class Header {
    private String title;
    // when title is set, logo is set to false
    private boolean isTitleRequired = false;
    // by default logo is enabled
    private boolean isLogoRequired = true;

    @BindingAdapter("visibleIf")
    public static void changeVisibility(@NonNull View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE: View.GONE);
    }

    public boolean isTitleRequired() {
        return this.isTitleRequired;
    }

    public void setTitleRequired(boolean titleRequired) {
        isTitleRequired = titleRequired;
    }

    public boolean isLogoRequired() {
        return isLogoRequired;
    }

    public void setLogoRequired(boolean logoRequired) {
        isLogoRequired = logoRequired;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if(title != null) {
            this.title = title;
            this.isTitleRequired = true;
            this.isLogoRequired = false;
        }
    }
}
