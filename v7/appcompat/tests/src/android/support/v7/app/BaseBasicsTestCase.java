/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v7.app;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.v7.testutils.TestUtilsActions.setSystemUiVisibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.v7.appcompat.test.R;
import android.support.v7.custom.FitWindowsContentLayout;
import android.support.v7.testutils.BaseTestActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.View;
import android.view.WindowInsets;

import org.junit.Test;

@SmallTest
public abstract class BaseBasicsTestCase<A extends BaseTestActivity>
        extends BaseInstrumentationTestCase<A> {

    protected BaseBasicsTestCase(Class<A> activityClass) {
        super(activityClass);
    }

    @Test
    public void testActionBarExists() {
        assertNotNull("ActionBar is not null", getActivity().getSupportActionBar());
    }

    @Test
    public void testDefaultActionBarTitle() {
        assertEquals(getActivity().getTitle(), getActivity().getSupportActionBar().getTitle());
    }

    @UiThreadTest
    @Test
    public void testSetActionBarTitle() {
        final String newTitle = "hello";
        mActivityTestRule.getActivity().setTitle(newTitle);
        assertEquals("New title is set to ActionBar",
                newTitle, mActivityTestRule.getActivity().getSupportActionBar().getTitle());
    }

    @Test
    @SdkSuppress(minSdkVersion = 16)
    public void testFitSystemWindowsReachesContent() {
        final FitWindowsContentLayout content =
                (FitWindowsContentLayout) getActivity().findViewById(R.id.test_content);
        assertNotNull(content);

        // Call setSystemUiVisibility with flags which will cause window insets to be dispatched
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        onView(withId(R.id.test_content)).perform(setSystemUiVisibility(flags));

        assertTrue(content.getFitsSystemWindowsCalled());
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testOnApplyWindowInsetsReachesContent() {
        final View content = getActivity().findViewById(R.id.test_content);
        assertNotNull(content);

        // Create a spy of one of our test listener and set it on our content
        final View.OnApplyWindowInsetsListener spyListener
                = spy(new TestOnApplyWindowInsetsListener());
        content.setOnApplyWindowInsetsListener(spyListener);

        // Call setSystemUiVisibility with flags which will cause window insets to be dispatched
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        onView(withId(R.id.test_content)).perform(setSystemUiVisibility(flags));

        // Verify that the listener was called at least once
        verify(spyListener, atLeastOnce())
                .onApplyWindowInsets(eq(content), any(WindowInsets.class));
    }

    @Test
    @UiThreadTest
    public void testSupportActionModeCallbacks() {
        final A activity = getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback callback = mock(ActionMode.Callback.class);
        when(callback.onCreateActionMode(any(ActionMode.class), any(Menu.class))).thenReturn(true);

        // Start an action mode
        final ActionMode actionMode = activity.startSupportActionMode(callback);
        assertNotNull(actionMode);

        // Now verify that onCreateActionMode and onPrepareActionMode are called once
        verify(callback).onCreateActionMode(any(ActionMode.class), any(Menu.class));
        verify(callback).onPrepareActionMode(any(ActionMode.class), any(Menu.class));

        // Now finish and verify that onDestroyActionMode is called once, and there are no more
        // interactions
        actionMode.finish();
        verify(callback).onDestroyActionMode(any(ActionMode.class));
        verifyNoMoreInteractions(callback);
    }

    @Test
    @UiThreadTest
    public void testSupportActionModeCallbacksInvalidate() {
        final A activity = getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback callback = mock(ActionMode.Callback.class);
        when(callback.onCreateActionMode(any(ActionMode.class), any(Menu.class))).thenReturn(true);

        // Start an action mode
        final ActionMode actionMode = activity.startSupportActionMode(callback);
        // Assert that one was created
        assertNotNull(actionMode);
        // Reset the mock so that any callback counts from the create are reset
        reset(callback);

        // Now invalidate the action mode
        actionMode.invalidate();

        // Now verify that onCreateActionMode is not called, and onPrepareActionMode is called once
        verify(callback, never()).onCreateActionMode(any(ActionMode.class), any(Menu.class));
        verify(callback).onPrepareActionMode(any(ActionMode.class), any(Menu.class));
    }

    @Test
    @UiThreadTest
    public void testSupportActionModeCallbacksWithFalseOnCreate() {
        final A activity = getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback callback = mock(ActionMode.Callback.class);
        when(callback.onCreateActionMode(any(ActionMode.class), any(Menu.class))).thenReturn(false);

        // Start an action mode
        final ActionMode actionMode = activity.startSupportActionMode(callback);

        // Now verify that onCreateActionMode is called once
        verify(callback).onCreateActionMode(any(ActionMode.class), any(Menu.class));

        // Now verify that onPrepareActionMode is not called (since onCreateActionMode
        // returns false)
        verify(callback, never()).onPrepareActionMode(any(ActionMode.class), any(Menu.class));

        // Assert that an action mode was not created
        assertNull(actionMode);
    }

    protected void testSupportActionModeAppCompatCallbacks(final boolean fromWindow) {
        final A activity = getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback amCallback = mock(ActionMode.Callback.class);
        when(amCallback.onCreateActionMode(any(ActionMode.class), any(Menu.class)))
                .thenReturn(true);

        // Create a mock AppCompatCallback, which returns null from
        // onWindowStartingSupportActionMode, and set it on the Activity
        final AppCompatCallback apCallback = mock(AppCompatCallback.class);
        when(apCallback.onWindowStartingSupportActionMode(any(ActionMode.Callback.class)))
                .thenReturn(null);
        activity.setAppCompatCallback(apCallback);

        // Start an action mode with the action mode callback
        final ActionMode actionMode = activity.startSupportActionMode(amCallback);

        if (fromWindow) {
            // Verify that the callback's onWindowStartingSupportActionMode was called
            verify(apCallback).onWindowStartingSupportActionMode(any(ActionMode.Callback.class));
        }

        // Now assert that an action mode was created
        assertNotNull(actionMode);

        // Now verify that onSupportActionModeStarted is called once
        verify(apCallback).onSupportActionModeStarted(any(ActionMode.class));

        // Now finish and verify that onDestroyActionMode is called once
        actionMode.finish();
        verify(apCallback).onSupportActionModeFinished(any(ActionMode.class));
    }

    public static class TestOnApplyWindowInsetsListener
            implements View.OnApplyWindowInsetsListener {
        @Override
        public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
            return windowInsets;
        }
    }
}
