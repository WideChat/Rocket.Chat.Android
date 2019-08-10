package chat.rocket.android.chatroom.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import chat.rocket.android.R

class MessageFlexLayout : RelativeLayout {
    private var viewPrimary: TextView? = null
    private var viewSecondary: View? = null
    private var maxWidth: Float = 0F

    private lateinit var attributes: TypedArray

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        attributes = context.obtainStyledAttributes(attrs, R.styleable.MessageFlexLayout, 0, 0)
        val defaultMaxWidth = (260 * resources.displayMetrics.density).toInt()
        maxWidth = attributes.getDimensionPixelSize(
            R.styleable.MessageFlexLayout_maxWidth,
            defaultMaxWidth
        ).toFloat()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        try {
            viewPrimary =
                this.findViewById(
                    attributes.getResourceId(
                        R.styleable.MessageFlexLayout_viewPrimary,
                        -1
                    )
                )
            viewSecondary =
                this.findViewById(
                    attributes.getResourceId(
                        R.styleable.MessageFlexLayout_viewSecondary,
                        -1
                    )
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var availableWidth = widthSize - paddingLeft - paddingRight
        var availableHeight = heightSize - paddingTop - paddingBottom

        /*viewPrimary!!.measure(
            MeasureSpec.makeMeasureSpec(maxWidth.toInt(), MeasureSpec.AT_MOST),
            heightMeasureSpec
        )*/

        //availableWidth = min(availableWidth, maxWidth.toInt())

        if (viewPrimary == null || viewSecondary == null || widthSize <= 0) {
            return
        }

        val viewPrimaryLayoutParams = viewPrimary!!.layoutParams as LayoutParams
        val viewPrimaryWidth =
            viewPrimary!!.measuredWidth + viewPrimaryLayoutParams.leftMargin + viewPrimaryLayoutParams.rightMargin
        val viewPrimaryHeight =
            viewPrimary!!.measuredHeight + viewPrimaryLayoutParams.topMargin + viewPrimaryLayoutParams.bottomMargin

        val viewSecondaryLayoutParams = viewSecondary!!.layoutParams as LayoutParams
        val viewSecondaryWidth =
            viewSecondary!!.measuredWidth + viewSecondaryLayoutParams.leftMargin + viewSecondaryLayoutParams.rightMargin
        val viewSecondaryHeight =
            viewSecondary!!.measuredHeight + viewSecondaryLayoutParams.topMargin + viewSecondaryLayoutParams.bottomMargin

        val viewPrimaryLineCount = viewPrimary!!.lineCount
        val viewPrimaryLastLineWidth =
            if (viewPrimaryLineCount > 0) viewPrimary!!.layout.getLineWidth(viewPrimaryLineCount - 1) else 0F

        widthSize = paddingLeft + paddingRight
        heightSize = paddingTop + paddingBottom

        if (viewPrimaryLineCount > 0 && (viewPrimaryLastLineWidth + viewSecondaryWidth) < viewPrimary!!.measuredWidth) {
            widthSize += viewPrimaryWidth
            heightSize += viewPrimaryHeight
        } else if (viewPrimaryLineCount > 0 && (viewPrimaryLastLineWidth + viewSecondaryWidth) >= availableWidth) {
            widthSize += viewPrimaryWidth
            heightSize += viewPrimaryHeight + viewSecondaryHeight
        } else {
            widthSize += viewPrimaryWidth + viewSecondaryWidth
            heightSize += viewPrimaryHeight
        }

        this.setMeasuredDimension(widthSize, heightSize)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (viewPrimary == null || viewSecondary == null) {
            return
        }

        viewPrimary!!.layout(
            paddingLeft,
            paddingTop,
            viewPrimary!!.width + paddingLeft,
            viewPrimary!!.height + paddingTop
        )

        viewSecondary!!.layout(
            right - left - viewSecondary!!.width - paddingRight,
            bottom - top - paddingBottom - viewSecondary!!.height,
            right - left - paddingRight,
            bottom - top - paddingBottom
        )
    }
}
