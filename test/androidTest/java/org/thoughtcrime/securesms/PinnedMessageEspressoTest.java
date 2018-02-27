package org.thoughtcrime.securesms;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.thoughtcrime.securesms.espresso.Helper;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PinnedMessageEspressoTest {
    @Rule
    public ActivityTestRule<ConversationListActivity> mainActivityRule =
            new ActivityTestRule(ConversationListActivity.class, true, false);

    @Test
    public void pageExists() {
        Helper helper = new Helper(mainActivityRule);

        helper
            .goConversations()
            .goConversation()
            .goPinned();
    }


    @Test
    public void pageDisplaysOwnNumber() {
        Helper helper = new Helper(mainActivityRule);

        helper
            .goConversations()
            .goConversation()
            .goPinned()
                .assertText(helper.getPhoneNumber());
    }

    @Test
    public void selectedMessageCanBePinned() {
        Helper helper = new Helper(mainActivityRule);

        helper
            .goConversations()
            .goConversation()
                .sendMessage("Hello World!")
                .selectMessage(0)
                .assertId(R.id.menu_context_pin_message);
    }

    @Test
    public void selectedPinnedMessageCanBeUnpinned() {
        Helper helper = new Helper(mainActivityRule);

        helper
            .goConversations()
            .goConversation()
                .sendMessage("Hello World!")
                .pinMessage(0)
                .selectMessage(0)
                .assertId(R.id.menu_context_unpin_message);
    }

    @Test
    public void canSeePinnedMessages() {
        Helper helper = new Helper(mainActivityRule);

        String testString = helper.randString();

        helper
            .goConversations()
            .goConversation()
                .sendMessage(testString)
                .pinMessage(1)
            .goPinned()
                .assertText(testString);
    }


    @Test
    public void unpinnedMessagesAreNotShown() {
        Helper helper = new Helper(mainActivityRule);

        String testString = helper.randString();

        helper
            .goConversations()
            .goConversation()
                .sendMessage(testString)
                .pinMessage(0)
            .goPinned()
                .assertText(testString)
            .goConversation()
                .unpinMessage(0)
            .goPinned()
                .assertNoText(testString);
    }


    @Test
    public void canUnpinMessagesFromList() {
        Helper helper = new Helper(mainActivityRule);

        String testString = helper.randString();

        helper
            .goConversations()
            .goConversation()
                .sendMessage(testString)
                .pinMessage(0)
            .goPinned()
                .assertText(testString)
                .unpinMessage(0)
                .assertNoText(testString);
    }

    @Test
    public void pinnedMessagesHaveIndicator() {
        Helper helper = new Helper(mainActivityRule);

        helper
            .goConversations()
            .goConversation()
                .sendMessage("Hello World!")
                .pinMessage(1)
                .assertIdAt(R.id.pinned_indicator, 1, true);
    }

    @Test
    public void unpinnedMessagesDoesNotHaveIndicator() {
        Helper helper = new Helper(mainActivityRule);

        helper
            .goConversations()
            .goConversation()
                .sendMessage("Hello World!")
                .pinMessage(1)
                .assertIdAt(R.id.pinned_indicator, 1, true)
                .unpinMessage(1)
                .assertNoIdAt(R.id.pinned_indicator, 1, true);
    }
}