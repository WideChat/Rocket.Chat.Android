package chat.rocket.android.chatroom.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import chat.rocket.android.R

class MessageFlexLayout : RelativeLayout {
    private var viewMain: TextView? = null
    private var viewSlave: View? = null

    private lateinit var attributes: TypedArray

    private var viewMainLayoutParams: LayoutParams? = null
    private var viewMainWidth: Int = 0
    private var viewMainHeight: Int = 0

    private var viewSlaveLayoutParams: LayoutParams? = null
    private var viewSlaveWidth: Int = 0
    private var viewSlaveHeight: Int = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        attributes = context.obtainStyledAttributes(attrs, R.styleable.MessageFlexLayout, 0, 0)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        try {
            viewMain = this.findViewById<View>(
                attributes.getResourceId(
                    R.styleable.MessageFlexLayout_viewMain,
                    -1
                )
            ) as TextView
            viewSlave = this.findViewById<View>(
                attributes.getResourceId(
                    R.styleable.MessageFlexLayout_viewSlave,
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

        if (viewMain == null || viewSlave == null || widthSize <= 0) {
            return
        }

        val availableWidth = widthSize - paddingLeft - paddingRight
        val availableHeight = heightSize - paddingTop - paddingBottom

        viewMainLayoutParams = viewMain!!.layoutParams as LayoutParams
        viewMainWidth =
            viewMain!!.measuredWidth + viewMainLayoutParams!!.leftMargin + viewMainLayoutParams!!.rightMargin
        viewMainHeight =
            viewMain!!.measuredHeight + viewMainLayoutParams!!.topMargin + viewMainLayoutParams!!.bottomMargin

        viewSlaveLayoutParams = viewSlave!!.layoutParams as LayoutParams?
        viewSlaveWidth =
            viewSlave!!.measuredWidth + viewSlaveLayoutParams!!.leftMargin + viewSlaveLayoutParams!!.rightMargin
        viewSlaveHeight =
            viewSlave!!.measuredHeight + viewSlaveLayoutParams!!.topMargin + viewSlaveLayoutParams!!.bottomMargin

        val viewMainLineCount = viewMain!!.lineCount
        val viewMainLastLineWidth = when (viewMainLineCount > 0) {
            true -> viewMain!!.layout.getLineWidth(viewMainLineCount - 1)
            else -> 0F
        }

        widthSize = paddingLeft + paddingRight
        heightSize = paddingTop + paddingBottom

        if (viewMainLineCount > 0 && (viewMainLastLineWidth + viewSlaveWidth < viewMain!!.measuredWidth)) {
            widthSize += viewMainWidth
            heightSize += viewMainHeight
        } else if (viewMainLineCount > 0 && (viewMainLastLineWidth + viewSlaveWidth >= availableWidth)) {
            widthSize += viewMainWidth
            heightSize += viewMainHeight + viewSlaveHeight
        } else {
            widthSize += viewMainWidth + viewSlaveWidth
            heightSize += viewMainHeight
        }

        this.setMeasuredDimension(widthSize, heightSize)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (viewMain == null || viewSlave == null) {
            return
        }

        viewMain!!.layout(
            paddingLeft,
            paddingTop,
            viewMain!!.width + paddingLeft,
            viewMain!!.height + paddingTop
        )

        viewSlave!!.layout(
            right - left - viewSlaveWidth - paddingRight,
            bottom - top - paddingBottom - viewSlaveHeight,
            right - left - paddingRight,
            bottom - top - paddingBottom
        )
    }
}
