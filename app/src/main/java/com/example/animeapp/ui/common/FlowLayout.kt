package com.example.animeapp.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import kotlin.math.roundToInt

class FlowLayout(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private var lineHeight = 0
    private val lines = mutableListOf<List<View>>()
    private val horizontalSpacingDp = 4f
    private val horizontalSpacingPx: Int by lazy {
        (horizontalSpacingDp * resources.displayMetrics.density).roundToInt()
    }

    private var isLoading = false
    private lateinit var progressBar: ProgressBar

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var maxWidth = 0
        var maxHeight = 0
        var lineWidth = 0
        var lineHeight = 0

        if (isLoading) {
            maxHeight += progressBar.measuredHeight
        }

        val childViews = (0 until childCount).map { getChildAt(it) }
        lines.clear()
        var currentLine = mutableListOf<View>()

        for (child in childViews) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val childWidth = child.measuredWidth + horizontalSpacingPx
            val childHeight = child.measuredHeight

            if (lineWidth + childWidth > width) {
                maxWidth = maxWidth.coerceAtLeast(lineWidth)
                maxHeight += lineHeight
                lines.add(currentLine)
                currentLine = mutableListOf()
                lineWidth = 0
                lineHeight = 0
            }

            lineWidth += childWidth
            lineHeight = lineHeight.coerceAtLeast(childHeight)
            currentLine.add(child)
        }

        if (currentLine.isNotEmpty()) {
            maxWidth = maxWidth.coerceAtLeast(lineWidth)
            maxHeight += lineHeight
            lines.add(currentLine)
        }

        val resolvedWidth = if (widthMode == MeasureSpec.EXACTLY) width else maxWidth
        val resolvedHeight = if (heightMode == MeasureSpec.EXACTLY) height else maxHeight
        setMeasuredDimension(
            resolvedWidth + paddingLeft + paddingRight,
            resolvedHeight + paddingTop + paddingBottom
        )

        this.lineHeight = lineHeight
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var top = paddingTop

        for (line in lines) {
            var left = paddingLeft
            val totalChildWidth = line.sumOf { it.measuredWidth + horizontalSpacingPx }
            val availableWidth = width - paddingLeft - paddingRight
            val extraSpace = availableWidth - totalChildWidth

            if (line.size > 1) {
                val spacing = extraSpace.toFloat() / (line.size - 1).toFloat()

                for (element in line) {
                    val childWidth = element.measuredWidth
                    val childHeight = element.measuredHeight
                    element.layout(left, top, left + childWidth, top + childHeight)
                    left += childWidth + horizontalSpacingPx + spacing.toInt()
                }
            } else if (line.isNotEmpty()) {
                val child = line[0]
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                child.layout(left, top, left + childWidth, top + childHeight)

            }

            top += lineHeight
        }

        if (isLoading) {
            progressBar.layout(
                (width - progressBar.measuredWidth) / 2, // Center horizontally
                top,
                (width + progressBar.measuredWidth) / 2,
                top + progressBar.measuredHeight
            )
        }
    }

    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading

        if (isLoading) {
            if (!::progressBar.isInitialized) {
                progressBar = ProgressBar(context)
                addView(progressBar)
            }
            progressBar.visibility = View.VISIBLE
        } else {
            if (::progressBar.isInitialized) {
                progressBar.visibility = View.GONE
            }
        }

        requestLayout()
    }
}