/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.widget;

import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.rosemoe.sora.event.EditorKeyEvent;
import io.github.rosemoe.sora.event.EventManager;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.KeyBindingEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.lang.ILanguage;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.method.KeyMetaStates;

/**
 * Handles {@link KeyEvent}s in editor.
 *
 * <p>
 * <strong>This is for internal use only.</strong>
 * </p>
 *
 * @author Rose
 * @author Akash Yadav
 */
public class EditorKeyEventHandler {

    private static final String TAG = "EditorKeyEventHandler";
    private final CodeEditor editor;
    private final KeyMetaStates keyMetaStates;

    EditorKeyEventHandler(CodeEditor editor) {
        Objects.requireNonNull(editor, "Cannot setup KeyEvent with null editor instance.");
        this.editor = editor;
        this.keyMetaStates = new KeyMetaStates(editor);
    }

    /**
     * Check if the given {@link KeyEvent} is a key binding event.
     * {@link EditorKeyEventHandler#keyMetaStates} must be notified about the key event before this
     * method is called.
     *
     * @param keyCode The keycode.
     * @param event   The key event.
     * @return <code>true</code> if the event is a key binding event. <code>false</code> otherwise.
     */
    private boolean isKeyBindingEvent(int keyCode, KeyEvent event) {

        // These keys must be pressed for the key event to be a key binding event
        if (!(keyMetaStates.isShiftPressed() || keyMetaStates.isAltPressed() || event.isCtrlPressed())) {
            return false;
        }

        // Any alphabet key
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            return true;
        }

        // Other key combinations
        return keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_MOVE_HOME
                || keyCode == KeyEvent.KEYCODE_MOVE_END;
    }

    /**
     * Get the {@link KeyMetaStates} instance.
     *
     * @return The {@link KeyMetaStates} instance.
     */
    @NonNull
    public KeyMetaStates getKeyMetaStates() {
        return keyMetaStates;
    }

    /**
     * Called by editor in {@link CodeEditor#onKeyDown(int, KeyEvent)}.
     *
     * @param keyCode The key code.
     * @param event   The key event.
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        keyMetaStates.onKeyDown(event);
        final CodeEditor editor = this.editor;
        final EventManager eventManager = editor.eventManager;

        final EditorKeyEvent editorKeyEvent = new EditorKeyEvent(editor, event, EditorKeyEvent.Type.DOWN);
        final KeyBindingEvent keybindingEvent =
                new KeyBindingEvent(editor,
                        event,
                        EditorKeyEvent.Type.DOWN,
                        editor.canHandleKeyBinding(keyCode, event.isCtrlPressed(), keyMetaStates.isShiftPressed(), keyMetaStates.isAltPressed()));
        if ((eventManager.dispatchEvent(editorKeyEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
            return editorKeyEvent.result(false);
        }

        final boolean isShiftPressed = keyMetaStates.isShiftPressed();
        final boolean isAltPressed = keyMetaStates.isAltPressed();
        final boolean isCtrlPressed = event.isCtrlPressed();

        // Currently, KeyBindingEvent is triggered only for (Shift | Ctrl | Alt) + alphabet keys
        // Should we add support for more keys?
        if (isKeyBindingEvent(keyCode, event)) {
            if ((eventManager.dispatchEvent(keybindingEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || editorKeyEvent.result(false);
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MOVE_HOME:
            case KeyEvent.KEYCODE_MOVE_END:
                keyMetaStates.adjust();
        }

        Boolean result = handleKeyEvent(event, editorKeyEvent, keybindingEvent, keyCode, isShiftPressed, isAltPressed, isCtrlPressed);
        if (result != null) {
            return editorKeyEvent.result(result);
        }

        return editorKeyEvent.result(editor.onSuperKeyDown(keyCode, event));
    }

    private Boolean handleKeyEvent(KeyEvent event,
                                   EditorKeyEvent editorKeyEvent,
                                   KeyBindingEvent keybindingEvent,
                                   int keyCode,
                                   boolean isShiftPressed,
                                   boolean isAltPressed,
                                   boolean isCtrlPressed
    ) {
        final EditorInputConnection connection = editor.inputConnection;
        final Cursor editorCursor = editor.getCursor();
        final Content editorText = editor.getText();
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (editorCursor.isSelected()) {
                    editor.setSelection(editorCursor.getLeftLine(), editorCursor.getLeftColumn());
                    return editorKeyEvent.result(true);
                }
                if (editor.isInLongSelect()) {
                    editor.endLongSelect();
                    return editorKeyEvent.result(true);
                }
                return editorKeyEvent.result(false);
            }
            case KeyEvent.KEYCODE_DEL:
                if (editor.isEditable()) {
                    editor.deleteText();
                    editor.notifyIMEExternalCursorChange();
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_FORWARD_DEL: {
                if (editor.isEditable()) {
                    connection.deleteSurroundingText(0, 1);
                    editor.notifyIMEExternalCursorChange();
                }
                return editorKeyEvent.result(true);
            }
            case KeyEvent.KEYCODE_ENTER: {
                return handleEnterKeyEvent(editorKeyEvent, keybindingEvent, isShiftPressed, isAltPressed, isCtrlPressed);
            }
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isCtrlPressed) {
                    if (isShiftPressed) {
                        final CharPosition left = editorCursor.left();
                        final CharPosition right = editorCursor.right();
                        final int lines = editorText.getLineCount();
                        if (right.getLine() == lines - 1) {
                            // last line, cannot move down
                            return editorKeyEvent.result(true);
                        }

                        final String next = editorText.getLine(right.getLine() + 1).toString();
                        editorText.beginBatchEdit();
                        editorText.delete(right.getLine(), editorText.getColumnCount(right.getLine()), right.getLine() + 1, next.length());
                        editorText.insert(left.getLine(), 0, next.concat(editor.getLineSeparator().getChar()));
                        editorText.endBatchEdit();

                        // Update selection
                        final CharPosition newLeft = editorText.getIndexer().getCharPosition(left.getLine() + 1, left.getColumn());
                        final CharPosition newRight = editorText.getIndexer().getCharPosition(right.getLine() + 1, right.getColumn());
                        if (left.getIndex() != right.getIndex()) {
                            CharPosition backupAnchor = editor.selectionAnchor;
                            editor.setSelectionRegion(newLeft.getLine(), newLeft.getColumn(),
                                    newRight.getLine(), newRight.getColumn());
                            if (backupAnchor != null) {
                                if (backupAnchor.equals(left)) {
                                    editor.selectionAnchor = newLeft;
                                } else {
                                    editor.selectionAnchor = newRight;
                                }
                            }
                        } else {
                            editor.setSelection(newLeft.getLine(), newLeft.getColumn());
                        }

                        return editorKeyEvent.result(true);
                    }
                    editor.getEventHandler().scrollBy(0, editor.getRowHeight());
                    return editorKeyEvent.result(true);
                }
                editor.moveOrExtendSelection(SelectionMovement.DOWN, isShiftPressed);
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_DPAD_UP:
                if (isCtrlPressed) {
                    if (isShiftPressed) {
                        final CharPosition left = editorCursor.left();
                        final CharPosition right = editorCursor.right();
                        if (left.getLine() == 0) {
                            // first line, cannot move up
                            return editorKeyEvent.result(true);
                        }

                        final String prev = editorText.getLine(left.getLine() - 1).toString();
                        editorText.beginBatchEdit();
                        editorText.delete(left.getLine() - 1, 0, left.getLine(), 0);
                        editorText.insert(right.getLine() - 1, editorText.getColumnCount(right.getLine() - 1), editor.getLineSeparator().getContent().concat(prev));
                        editorText.endBatchEdit();

                        // Update selection
                        final CharPosition newLeft = editorText.getIndexer().getCharPosition(left.getLine() - 1, left.getColumn());
                        final CharPosition newRight = editorText.getIndexer().getCharPosition(right.getLine() - 1, right.getColumn());
                        if (left.getIndex() != right.getIndex()) {
                            CharPosition backupAnchor = editor.selectionAnchor;
                            editor.setSelectionRegion(newLeft.getLine(), newLeft.getColumn(), newRight.getLine(), newRight.getColumn());
                            if (backupAnchor != null) {
                                if (backupAnchor.equals(left)) {
                                    editor.selectionAnchor = newLeft;
                                } else {
                                    editor.selectionAnchor = newRight;
                                }
                            }
                        } else {
                            editor.setSelection(newLeft.getLine(), newLeft.getColumn());
                        }

                        return editorKeyEvent.result(true);
                    }
                    editor.getEventHandler().scrollBy(0, -editor.getRowHeight());
                    return editorKeyEvent.result(true);
                }
                editor.moveOrExtendSelection(SelectionMovement.UP, isShiftPressed);
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.PREVIOUS_WORD_BOUNDARY, isShiftPressed);
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.LEFT, isShiftPressed);
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.NEXT_WORD_BOUNDARY, isShiftPressed);
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.RIGHT, isShiftPressed);
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_MOVE_END:
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.TEXT_END, isShiftPressed);
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.LINE_END, isShiftPressed);
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_MOVE_HOME:
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.TEXT_START, isShiftPressed);
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.LINE_START, isShiftPressed);
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_PAGE_DOWN:
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_BOTTOM, isShiftPressed);
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_DOWN, isShiftPressed);
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_PAGE_UP:
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_TOP, isShiftPressed);
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_UP, isShiftPressed);
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_TAB:
                if (editor.isEditable()) {
                    if (!isAltPressed && !isCtrlPressed) {
                        if (editor.getSnippetController().isInSnippet()) {
                            if (isShiftPressed) {
                                editor.getSnippetController().shiftToPreviousTabStop();
                            } else {
                                editor.getSnippetController().shiftToNextTabStop();
                            }
                        } else {
                            if (isShiftPressed) {
                                // Shift + TAB -> unindent the [selected] lines
                                editor.unindentSelection();
                            } else {
                                editor.indentOrCommitTab();
                            }
                        }
                    }
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_PASTE:
                if (editor.isEditable()) {
                    editor.pasteText();
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_COPY:
                editor.copyText();
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_SPACE:
                if (editor.isEditable()) {
                    editor.commitText(" ");
                    editor.notifyIMEExternalCursorChange();
                }
                return editorKeyEvent.result(true);
            case KeyEvent.KEYCODE_ESCAPE:
                if (editorCursor.isSelected()) {
                    final CharPosition newPosition = editor.getProps().positionOfCursorWhenExitSelecting ?
                            editorCursor.right() : editorCursor.left();
                    editor.setSelection(newPosition.getLine(), newPosition.getColumn(), true);
                }
                return editorKeyEvent.result(true);
            default:
                if (event.isCtrlPressed() && !event.isAltPressed()) {
                    return handleCtrlKeyBinding(editorKeyEvent, keybindingEvent, keyCode, isShiftPressed);
                }

                if (!event.isCtrlPressed() && !event.isAltPressed()) {
                    return handlePrintingKey(event, editorKeyEvent, keyCode);
                }
        }
        return null;
    }

    @NonNull
    private Boolean handlePrintingKey(
            KeyEvent event,
            EditorKeyEvent editorKeyEvent,
            int keyCode) {
        final Content editorText = this.editor.getText();
        final Cursor editorCursor = this.editor.getCursor();
        int charCode = event.getUnicodeChar(keyMetaStates.getMetaState(event));
        if (charCode != 0 && editor.isEditable()) {
            if (charCode == KeyCharacterMap.HEX_INPUT || charCode == KeyCharacterMap.PICKER_DIALOG_INPUT) {
                // unsupported: character picker dialog and hex input
                return editor.onSuperKeyDown(keyCode, event);
            }
            // #547 Dead Chars
            boolean dead = false;
            if ((charCode & KeyCharacterMap.COMBINING_ACCENT) != 0) {
                charCode = charCode & KeyCharacterMap.COMBINING_ACCENT_MASK;
                dead = true;
            }
            if (dead) {
                Cursor cursor = editor.getCursor();
                if (event.getRepeatCount() == 0) {
                    if (cursor.getRightColumn() > 0) {
                        int charCount = Character.charCount(Character.codePointBefore(editor.getText().getLine(cursor.getRightLine()), cursor.getRightColumn()));
                        editor.setSelectionRegion(cursor.getRightLine(), cursor.getRightColumn() - charCount, cursor.getRightLine(), cursor.getRightColumn(), SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE);
                        return true;
                    } // Otherwise, insert the char directly
                } else if (event.getRepeatCount() == 1) {
                    // Try to combine the two characters
                    if (cursor.isSelected() && cursor.getRightColumn() > 0) {
                        int base = Character.codePointBefore(editor.getText().getLine(cursor.getRightLine()), cursor.getRightColumn());
                        int charCount = Character.charCount(base);
                        if (charCount == cursor.getRight() - cursor.getLeft()) {
                            int composed = KeyCharacterMap.getDeadChar(charCode, base);
                            if (composed != base) {
                                editor.commitText(String.valueOf(Character.toChars(composed)));
                            } else {
                                editor.setSelection(cursor.getRightLine(), cursor.getRightColumn());
                            }
                            editor.notifyIMEExternalCursorChange();
                            return true;
                        }
                    } // Otherwise, insert the char directly
                } else {
                    // Repeated key strokes are ignored
                    return true;
                }
            }

            String text = new String(Character.toChars(charCode));

            editor.commitText(text);
            editor.notifyIMEExternalCursorChange();

        } else {
            return editor.onSuperKeyDown(keyCode, event);
        }
        return editorKeyEvent.result(true);
    }

    @Nullable
    private Boolean handleCtrlKeyBinding(
            EditorKeyEvent e,
            KeyBindingEvent keybindingEvent,
            int keyCode,
            boolean isShiftPressed) {
        final CodeEditor editor = this.editor;
        final EditorInputConnection connection = editor.inputConnection;
        final Content editorText = editor.getText();
        final Cursor editorCursor = editor.getCursor();
        boolean editorResult = true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_V:
                if (editor.isEditable()) {
                    editor.pasteText();
                }
                break;
            case KeyEvent.KEYCODE_C:
                editor.copyText();
                break;
            case KeyEvent.KEYCODE_X:
                if (editor.isEditable()) {
                    editor.cutText();
                } else {
                    editor.copyText();
                }
                break;
            case KeyEvent.KEYCODE_A:
                editor.selectAll();
                break;
            case KeyEvent.KEYCODE_Z:
                if (editor.isEditable()) {
                    editor.undo();
                }
                break;
            case KeyEvent.KEYCODE_Y:
                if (editor.isEditable()) {
                    editor.redo();
                }
                break;
            case KeyEvent.KEYCODE_D:
                if (editor.isEditable()) {
                    editor.duplicateLine();
                }
                break;
            case KeyEvent.KEYCODE_W:
                editor.selectCurrentWord();
                break;
            case KeyEvent.KEYCODE_J:
                if (!isShiftPressed || editorCursor.isSelected()) {
                    // TODO If the cursor is selected, then the selected lines must be joined.
                    editorResult = false;
                    break;
                }

                final int line = editorCursor.getLeftLine();
                editor.setSelection(line, editorText.getColumnCount(line));
                connection.deleteSurroundingText(0, 1);
                editor.ensureSelectionVisible();
                break;
            default:
                return null;
        }
        return keybindingEvent.result(editorResult) || e.result(editorResult);
    }

    @NonNull
    private Boolean handleEnterKeyEvent(
            EditorKeyEvent editorKeyEvent,
            KeyBindingEvent keybindingEvent,
            boolean isShiftPressed,
            boolean isAltPressed,
            boolean isCtrlPressed) {
        final CodeEditor editor = this.editor;
        final Cursor editorCursor = editor.getCursor();
        final Content editorText = editor.getText();
        if (editor.isEditable()) {
            String lineSeparator = editor.getLineSeparator().getChar();
            final ILanguage editorLanguage = editor.getEditorLanguage();

            if (isShiftPressed && !isAltPressed && !isCtrlPressed) {
                // Shift + Enter
                return startNewLine(editor, editorCursor, editorText, editorKeyEvent, keybindingEvent);
            }

            if (isCtrlPressed && !isShiftPressed) {
                if (isAltPressed) {
                    // Ctrl + Alt + Enter
                    int line = editorCursor.left().getLine();
                    if (line == 0) {
                        editorText.insert(0, 0, lineSeparator);
                        editor.setSelection(0, 0);
                        editor.ensureSelectionVisible();
                        return keybindingEvent.result(true) || editorKeyEvent.result(true);
                    } else {
                        line--;
                        editor.setSelection(line, editorText.getColumnCount(line));
                        return startNewLine(editor, editorCursor, editorText, editorKeyEvent, keybindingEvent);
                    }
                }

                // Ctrl + Enter
                final CharPosition left = editorCursor.left().copy();
                editor.commitText(lineSeparator);
                editor.setSelection(left.getLine(), left.getColumn());
                editor.ensureSelectionVisible();
                return keybindingEvent.result(true) || editorKeyEvent.result(true);
            }

            NewlineHandler[] handlers = editorLanguage.getNewlineHandlers();
            if (handlers == null || editorCursor.isSelected()) {
                editor.commitText(lineSeparator, true);
            } else {
                boolean consumed = false;
                for (NewlineHandler handler : handlers) {
                    if (handler != null) {
                        if (handler.matchesRequirement(editorText, editorCursor.left(), editor.getStyles())) {
                            try {
                                NewlineHandleResult result = handler.handleNewline(editorText, editorCursor.left(), editor.getStyles(), editor.getTabWidth());
                                editor.commitText(result.text, false);
                                int delta = result.shiftLeft;
                                if (delta != 0) {
                                    int newSel = Math.max(editorCursor.getLeft() - delta, 0);
                                    CharPosition charPosition = editorCursor.getIndexer().getCharPosition(newSel);
                                    editor.setSelection(charPosition.getLine(), charPosition.getColumn());
                                }
                                consumed = true;
                            } catch (Exception ex) {
                                Log.w(TAG, "Error occurred while calling Language's NewlineHandler", ex);
                            }
                            break;
                        }
                    }
                }
                if (!consumed) {
                    editor.commitText(lineSeparator, true);
                }
            }
            editor.notifyIMEExternalCursorChange();
        }
        return editorKeyEvent.result(true);
    }

    private boolean startNewLine(CodeEditor editor, Cursor editorCursor, Content
            editorText, EditorKeyEvent e, KeyBindingEvent keybindingEvent) {
        final int line = editorCursor.right().getLine();
        editor.setSelection(line, editorText.getColumnCount(line));
        editor.commitText(editor.getLineSeparator().getChar());
        editor.ensureSelectionVisible();
        return keybindingEvent.result(true) || e.result(true);
    }

    /**
     * Called by editor in {@link CodeEditor#onKeyUp(int, KeyEvent)}.
     *
     * @param keyCode The key code.
     * @param event   The key event.
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        keyMetaStates.onKeyUp(event);

        final EventManager eventManager = this.editor.eventManager;
        final Cursor cursor = this.editor.getCursor();

        EditorKeyEvent e = new EditorKeyEvent(this.editor, event, EditorKeyEvent.Type.UP);
        if ((eventManager.dispatchEvent(e) & InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false);
        }

        if (isKeyBindingEvent(keyCode, event)) {
            final KeyBindingEvent keybindingEvent =
                    new KeyBindingEvent(editor,
                            event,
                            EditorKeyEvent.Type.UP,
                            editor.canHandleKeyBinding(keyCode, event.isCtrlPressed(), keyMetaStates.isShiftPressed(), keyMetaStates.isAltPressed()));

            if ((eventManager.dispatchEvent(keybindingEvent) & InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || e.result(false);
            }
        }

        return e.result(this.editor.onSuperKeyUp(keyCode, event));
    }

    /**
     * Called by editor in {@link CodeEditor#onKeyMultiple(int, int, KeyEvent)}.
     *
     * @param keyCode     The key code.
     * @param repeatCount The repeat count.
     * @param event       The key event.
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    public boolean onKeyMultiple(int keyCode, int repeatCount, @NonNull KeyEvent event) {
        final EditorKeyEvent e = new EditorKeyEvent(this.editor, event, EditorKeyEvent.Type.MULTIPLE);
        final EventManager eventManager = this.editor.eventManager;
        if ((eventManager.dispatchEvent(e) & InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false);
        }

        return e.result(this.editor.onSuperKeyMultiple(keyCode, repeatCount, event));
    }
}