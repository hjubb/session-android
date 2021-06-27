package org.thoughtcrime.securesms.conversation.v2

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.album_thumbnail_view.view.*
import network.loki.messenger.R
import org.thoughtcrime.securesms.MediaPreviewActivity
import org.thoughtcrime.securesms.components.CornerMask
import org.thoughtcrime.securesms.conversation.v2.utilities.KThumbnailView
import org.thoughtcrime.securesms.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.loki.utilities.ActivityDispatcher
import org.thoughtcrime.securesms.longmessage.LongMessageActivity
import org.thoughtcrime.securesms.mms.GlideRequests
import org.thoughtcrime.securesms.mms.Slide

class AlbumThumbnailView : FrameLayout {

    // region Lifecycle
    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    private val cornerMask by lazy { CornerMask(this) }
    private var slides: List<Slide> = listOf()
    private var slideSize: Int = 0

    private fun initialize() {
        LayoutInflater.from(context).inflate(R.layout.album_thumbnail_view, this)
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        cornerMask.mask(canvas)
    }

    // endregion

    // region Interaction

    fun calculateHitObject(rawRect: Rect, mms: MmsMessageRecord) {
        // Z-check in specific order
        val testRect = Rect()
        // test "Read More"
        albumCellBodyTextReadMore.getGlobalVisibleRect(testRect)
        if (Rect.intersects(rawRect, testRect)) {
            // dispatch to activity view
            ActivityDispatcher.get(context)?.dispatchIntent { context ->
                LongMessageActivity.getIntent(context, mms.recipient.address, mms.getId(), true)
            }
            return
        }
        // test each album child
        albumCellContainer.findViewById<ViewGroup>(R.id.album_thumbnail_root)?.children?.forEachIndexed { index, child ->
            child.getGlobalVisibleRect(testRect)
            if (Rect.intersects(rawRect, testRect)) {
                // hit intersects with this particular child
                val slide = slides.getOrNull(index) ?: return
                ActivityDispatcher.get(context)?.dispatchIntent { context ->
                    MediaPreviewActivity.getPreviewIntent(context, slide, mms)
                }
            }
        }
    }

    fun bind(glideRequests: GlideRequests, message: MmsMessageRecord,
             isStart: Boolean, isEnd: Boolean) {
        slides = message.slideDeck.thumbnailSlides
        if (slides.isEmpty()) {
            // this should never be encountered because it's checked by parent
            return
        }
        calculateRadius(isStart, isEnd, message.isOutgoing)

        // recreate cell views if different size to what we have already (for recycling)
        if (slides.size != this.slideSize) {
            albumCellContainer.removeAllViews()
            LayoutInflater.from(context).inflate(layoutRes(slides.size), albumCellContainer)
            this.slideSize = slides.size
        }
        // iterate binding
        slides.take(5).forEachIndexed { position, slide ->
            val thumbnailView = getThumbnailView(position)
            thumbnailView.setImageResource(glideRequests, slide, showControls = false, isPreview = false)
        }
        albumCellBodyParent.isVisible = message.body.isNotEmpty()
        albumCellBodyText.text = message.body
        post {
            // post to await layout of text
            albumCellBodyText.layout?.let { layout ->
                val maxEllipsis = (0 until layout.lineCount).maxByOrNull { lineNum -> layout.getEllipsisCount(lineNum) }
                        ?: 0
                // show read more text if at least one line is ellipsized
                albumCellBodyTextReadMore.isVisible = maxEllipsis > 0
            }
        }
    }

    // endregion


    fun layoutRes(slideCount: Int) = when (slideCount) {
        1 -> R.layout.album_thumbnail_1 // single
        2 -> R.layout.album_thumbnail_2// two sidebyside
        3 -> R.layout.album_thumbnail_3// three stacked
        4 -> R.layout.album_thumbnail_4// four square
        5 -> R.layout.album_thumbnail_5//
        else -> R.layout.album_thumbnail_many// five or more
    }

    fun getThumbnailView(position: Int): KThumbnailView = when (position) {
        0 -> albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_1)
        1 -> albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_2)
        2 -> albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_3)
        3 -> albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_4)
        4 -> albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_5)
        else -> throw Exception("Can't get thumbnail view for non-existent thumbnail at position: $position")
    }

    fun calculateRadius(isStart: Boolean, isEnd: Boolean, outgoing: Boolean) {
        val roundedDimen = context.resources.getDimension(R.dimen.message_corner_radius).toInt()
        val collapsedDimen = context.resources.getDimension(R.dimen.message_corner_collapse_radius).toInt()
        val (startTop, endTop, startBottom, endBottom) = when {
            // single message, consistent dimen
            isStart && isEnd -> intArrayOf(roundedDimen, roundedDimen, roundedDimen, roundedDimen)
            // start of message cluster, collapsed BL
            isStart -> intArrayOf(roundedDimen, roundedDimen, collapsedDimen, roundedDimen)
            // end of message cluster, collapsed TL
            isEnd -> intArrayOf(collapsedDimen, roundedDimen, roundedDimen, roundedDimen)
            // else in the middle, no rounding left side
            else -> intArrayOf(collapsedDimen, roundedDimen, collapsedDimen, roundedDimen)
        }
        // TL, TR, BR, BL (CW direction)
        cornerMask.setRadii(
                if (!outgoing) startTop else endTop, // TL
                if (!outgoing) endTop else startTop, // TR
                if (!outgoing) endBottom else startBottom, // BR
                if (!outgoing) startBottom else endBottom // BL
        )
    }

}