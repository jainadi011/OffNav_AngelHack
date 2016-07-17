package com.zriton.offnav.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by aditya on 16/7/16.
 */
public class TypefacedTextView extends TextView {

    public TypefacedTextView(final Context context, final AttributeSet attrs) {

        super(context, attrs);

        if (isInEditMode()) {
            return;
        }
        if (attrs != null) {
            final int typefaceCode = TypefaceUtils.typefaceCodeFromAttribute(context, attrs);

            final Typeface typeface = TypefaceCache
                            .get(context.getAssets(), typefaceCode);
            setTypeface(typeface);
        }
    }

    public TypefacedTextView(final Context context) {
        super(context);
    }
}
