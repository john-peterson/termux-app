package com.termux.shared.termux.terminal.io;

import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;
import com.termux.shared.termux.extrakeys.ExtraKeyButton;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.extrakeys.SpecialButton;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import static com.termux.shared.termux.extrakeys.ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS;

public class TerminalExtraKeys implements ExtraKeysView.IExtraKeysView {

    private final TerminalView mTerminalView;

    public TerminalExtraKeys(@NonNull TerminalView terminalView) {
        mTerminalView = terminalView;
    }

    @Override
    public void onExtraKeyButtonClick(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
        // Logger.logError("scroll", "extra key clicked");
        if (buttonInfo.isMacro()) {
            String[] keys = buttonInfo.getKey().split(" ");
            boolean ctrlDown = false;
            boolean altDown = false;
            boolean shiftDown = false;
            boolean fnDown = false;
            for (String key : keys) {
                if (SpecialButton.CTRL.getKey().equals(key)) {
                    ctrlDown = true;
                } else if (SpecialButton.ALT.getKey().equals(key)) {
                    altDown = true;
                } else if (SpecialButton.SHIFT.getKey().equals(key)) {
                    shiftDown = true;
                } else if (SpecialButton.FN.getKey().equals(key)) {
                    fnDown = true;
                } else {
                    onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
                    ctrlDown = false;
                    altDown = false;
                    shiftDown = false;
                    fnDown = false;
                }
            }
        } else {
            onTerminalExtraKeyButtonClick(view, buttonInfo.getKey(), false, false, false, false);
        }
    }

    protected void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        Logger.logError("scroll", "extra key clicked=" + key);
        if (PRIMARY_KEY_CODES_FOR_STRINGS.containsKey(key)) {
            Integer keyCode = PRIMARY_KEY_CODES_FOR_STRINGS.get(key);
            if (keyCode == null)
                return;
            int metaState = 0;
            if (ctrlDown)
                metaState |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
            if (altDown)
                metaState |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
            if (shiftDown)
                metaState |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
            if (fnDown)
                metaState |= KeyEvent.META_FUNCTION_ON;
            KeyEvent keyEvent = new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, metaState);
        Logger.logError("scroll", "submit extra key to view.client=" + keyCode);
            mTerminalView.onKeyDown(keyCode, keyEvent);
        } else {
            // not a control char
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                key.codePoints().forEach(codePoint -> {
        Logger.logError("scroll", "extra key submitted as code point clicked=" + codePoint);
                    mTerminalView.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, codePoint, ctrlDown, altDown);
                });
            } else {
                TerminalSession session = mTerminalView.getCurrentSession();
                if (session != null && key.length() > 0){
        Logger.logError("scroll", "extra key session.write=" + key);
                    session.write(key);
                }
            }
        }
    }

    @Override
    public boolean performExtraKeyButtonHapticFeedback(View view, ExtraKeyButton buttonInfo, MaterialButton button) {
        return false;
    }
}
