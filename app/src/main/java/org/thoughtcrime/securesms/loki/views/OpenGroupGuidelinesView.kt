package org.thoughtcrime.securesms.loki.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_open_group_guidelines.view.*
import network.loki.messenger.R
import org.thoughtcrime.securesms.conversation.ConversationActivity
import org.thoughtcrime.securesms.loki.activities.OpenGroupGuidelinesActivity
import org.thoughtcrime.securesms.loki.utilities.push

class OpenGroupGuidelinesView : FrameLayout {

    constructor(context: Context) : super(context) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setUpViewHierarchy()
    }

    private fun setUpViewHierarchy() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val contentView = inflater.inflate(R.layout.view_open_group_guidelines, null)
        addView(contentView)
        readButton.setOnClickListener {
            val activity = context as ConversationActivity
            val intent = Intent(activity, OpenGroupGuidelinesActivity::class.java)
            activity.push(intent)
        }
    }
}