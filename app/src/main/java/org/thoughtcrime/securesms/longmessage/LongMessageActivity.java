package org.thoughtcrime.securesms.longmessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.annimon.stream.Stream;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.components.ConversationItemFooter;
import org.thoughtcrime.securesms.linkpreview.LinkPreviewUtil;

import org.session.libsession.messaging.threads.Address;
import org.session.libsession.messaging.threads.recipients.Recipient;
import org.session.libsession.utilities.TextSecurePreferences;
import org.session.libsession.utilities.ThemeUtil;
import org.session.libsession.utilities.Util;
import org.session.libsession.utilities.views.Stub;

import java.util.Locale;

import network.loki.messenger.R;

public class LongMessageActivity extends PassphraseRequiredActionBarActivity {

  private static final String KEY_ADDRESS    = "address";
  private static final String KEY_MESSAGE_ID = "message_id";
  private static final String KEY_IS_MMS     = "is_mms";

  private static final int MAX_DISPLAY_LENGTH = 64 * 1024;

  private Stub<ViewGroup> sentBubble;
  private Stub<ViewGroup> receivedBubble;

  private LongMessageViewModel viewModel;

  public static Intent getIntent(@NonNull Context context, @NonNull Address conversationAddress, long messageId, boolean isMms) {
    Intent intent = new Intent(context, LongMessageActivity.class);
    intent.putExtra(KEY_ADDRESS, conversationAddress.serialize());
    intent.putExtra(KEY_MESSAGE_ID, messageId);
    intent.putExtra(KEY_IS_MMS, isMms);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    super.onCreate(savedInstanceState, ready);
    setContentView(R.layout.longmessage_activity);

    sentBubble     = new Stub<>(findViewById(R.id.longmessage_sent_stub));
    receivedBubble = new Stub<>(findViewById(R.id.longmessage_received_stub));

    initViewModel(getIntent().getLongExtra(KEY_MESSAGE_ID, -1), getIntent().getBooleanExtra(KEY_IS_MMS, false));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
    }

    return false;
  }

  private void initViewModel(long messageId, boolean isMms) {
    viewModel = new ViewModelProvider(this, new LongMessageViewModel.Factory(getApplication(), new LongMessageRepository(this), messageId, isMms))
                                  .get(LongMessageViewModel.class);

    viewModel.getMessage().observe(this, message -> {
      if (message == null) return;

      if (!message.isPresent()) {
        Toast.makeText(this, R.string.LongMessageActivity_unable_to_find_message, Toast.LENGTH_SHORT).show();
        finish();
        return;
      }


      if (message.get().getMessageRecord().isOutgoing()) {
        getSupportActionBar().setTitle(getString(R.string.LongMessageActivity_your_message));
      } else {
        Recipient recipient = message.get().getMessageRecord().getRecipient();
        String    name      = Util.getFirstNonEmpty(recipient.getName(), recipient.getProfileName(), recipient.getAddress().serialize()) ;
        getSupportActionBar().setTitle(getString(R.string.LongMessageActivity_message_from_s, name));
      }

      ViewGroup bubble;

      if (message.get().getMessageRecord().isOutgoing()) {
        bubble = sentBubble.get();
        bubble.getBackground().setColorFilter(ThemeUtil.getThemedColor(this, R.attr.message_sent_background_color), PorterDuff.Mode.MULTIPLY);
      } else {
        bubble = receivedBubble.get();
        bubble.getBackground().setColorFilter(ThemeUtil.getThemedColor(this, R.attr.message_received_background_color), PorterDuff.Mode.MULTIPLY);
      }

      TextView               text   = bubble.findViewById(R.id.longmessage_text);
      ConversationItemFooter footer = bubble.findViewById(R.id.longmessage_footer);

      String          trimmedBody = getTrimmedBody(message.get().getFullBody());
      SpannableString styledBody  = linkifyMessageBody(new SpannableString(trimmedBody));

      bubble.setVisibility(View.VISIBLE);
      text.setText(styledBody);
      text.setMovementMethod(LinkMovementMethod.getInstance());
      text.setTextSize(TypedValue.COMPLEX_UNIT_SP, TextSecurePreferences.getMessageBodyTextSize(this));
      footer.setMessageRecord(message.get().getMessageRecord(), Locale.getDefault());
    });
  }

  private String getTrimmedBody(@NonNull String text) {
    return text.length() <= MAX_DISPLAY_LENGTH ? text
                                               : text.substring(0, MAX_DISPLAY_LENGTH);
  }

  private SpannableString linkifyMessageBody(SpannableString messageBody) {
    int     linkPattern = Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS;
    boolean hasLinks    = Linkify.addLinks(messageBody, linkPattern);

    if (hasLinks) {
      Stream.of(messageBody.getSpans(0, messageBody.length(), URLSpan.class))
            .filterNot(url -> LinkPreviewUtil.isLegalUrl(url.getURL()))
            .forEach(messageBody::removeSpan);
    }
    return messageBody;
  }
}
