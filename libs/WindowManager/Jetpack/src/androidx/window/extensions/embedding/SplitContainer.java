/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import android.app.Activity;
import android.content.res.Configuration;
import android.util.Pair;
import android.util.Size;
import android.window.WindowContainerTransaction;

import androidx.annotation.NonNull;

/**
 * Client-side descriptor of a split that holds two containers.
 */
class SplitContainer {
    @NonNull
    private final TaskFragmentContainer mPrimaryContainer;
    @NonNull
    private final TaskFragmentContainer mSecondaryContainer;
    @NonNull
    private final SplitRule mSplitRule;
    @NonNull
    private SplitAttributes mSplitAttributes;

    SplitContainer(@NonNull TaskFragmentContainer primaryContainer,
            @NonNull Activity primaryActivity,
            @NonNull TaskFragmentContainer secondaryContainer,
            @NonNull SplitRule splitRule,
            @NonNull SplitAttributes splitAttributes) {
        mPrimaryContainer = primaryContainer;
        mSecondaryContainer = secondaryContainer;
        mSplitRule = splitRule;
        mSplitAttributes = splitAttributes;

        if (shouldFinishPrimaryWithSecondary(splitRule)) {
            if (mPrimaryContainer.getRunningActivityCount() == 1
                    && mPrimaryContainer.hasActivity(primaryActivity.getActivityToken())) {
                mSecondaryContainer.addContainerToFinishOnExit(mPrimaryContainer);
            } else {
                // Only adding the activity to be finished vs. the entire TaskFragment while
                // the secondary container exits because there are other unrelated activities in the
                // primary TaskFragment.
                mSecondaryContainer.addActivityToFinishOnExit(primaryActivity);
            }
        }
        if (shouldFinishSecondaryWithPrimary(splitRule)) {
            mPrimaryContainer.addContainerToFinishOnExit(mSecondaryContainer);
        }
    }

    @NonNull
    TaskFragmentContainer getPrimaryContainer() {
        return mPrimaryContainer;
    }

    @NonNull
    TaskFragmentContainer getSecondaryContainer() {
        return mSecondaryContainer;
    }

    @NonNull
    SplitRule getSplitRule() {
        return mSplitRule;
    }

    @NonNull
    SplitAttributes getSplitAttributes() {
        return mSplitAttributes;
    }

    /**
     * Updates the {@link SplitAttributes} to this container.
     * It is usually used when there's a folding state change or
     * {@link SplitController#onTaskFragmentParentInfoChanged(WindowContainerTransaction, int,
     * Configuration)}.
     */
    void setSplitAttributes(@NonNull SplitAttributes splitAttributes) {
        mSplitAttributes = splitAttributes;
    }

    @NonNull
    TaskContainer getTaskContainer() {
        return getPrimaryContainer().getTaskContainer();
    }

    /** Returns the minimum dimension pair of primary container and secondary container. */
    @NonNull
    Pair<Size, Size> getMinDimensionsPair() {
        return new Pair<>(mPrimaryContainer.getMinDimensions(),
                mSecondaryContainer.getMinDimensions());
    }

    boolean isPlaceholderContainer() {
        return (mSplitRule instanceof SplitPlaceholderRule);
    }

    @NonNull
    SplitInfo toSplitInfo() {
        return new SplitInfo(mPrimaryContainer.toActivityStack(),
                mSecondaryContainer.toActivityStack(), mSplitAttributes);
    }

    static boolean shouldFinishPrimaryWithSecondary(@NonNull SplitRule splitRule) {
        final boolean isPlaceholderContainer = splitRule instanceof SplitPlaceholderRule;
        final boolean shouldFinishPrimaryWithSecondary = (splitRule instanceof SplitPairRule)
                && ((SplitPairRule) splitRule).getFinishPrimaryWithSecondary()
                != SplitRule.FINISH_NEVER;
        return shouldFinishPrimaryWithSecondary || isPlaceholderContainer;
    }

    static boolean shouldFinishSecondaryWithPrimary(@NonNull SplitRule splitRule) {
        final boolean isPlaceholderContainer = splitRule instanceof SplitPlaceholderRule;
        final boolean shouldFinishSecondaryWithPrimary = (splitRule instanceof SplitPairRule)
                && ((SplitPairRule) splitRule).getFinishSecondaryWithPrimary()
                != SplitRule.FINISH_NEVER;
        return shouldFinishSecondaryWithPrimary || isPlaceholderContainer;
    }

    static boolean shouldFinishAssociatedContainerWhenStacked(int finishBehavior) {
        return finishBehavior == SplitRule.FINISH_ALWAYS;
    }

    static boolean shouldFinishAssociatedContainerWhenAdjacent(int finishBehavior) {
        return finishBehavior == SplitRule.FINISH_ALWAYS
                || finishBehavior == SplitRule.FINISH_ADJACENT;
    }

    static int getFinishPrimaryWithSecondaryBehavior(@NonNull SplitRule splitRule) {
        if (splitRule instanceof SplitPlaceholderRule) {
            return ((SplitPlaceholderRule) splitRule).getFinishPrimaryWithSecondary();
        }
        if (splitRule instanceof SplitPairRule) {
            return ((SplitPairRule) splitRule).getFinishPrimaryWithSecondary();
        }
        return SplitRule.FINISH_NEVER;
    }

    static int getFinishSecondaryWithPrimaryBehavior(@NonNull SplitRule splitRule) {
        if (splitRule instanceof SplitPlaceholderRule) {
            return SplitRule.FINISH_ALWAYS;
        }
        if (splitRule instanceof SplitPairRule) {
            return ((SplitPairRule) splitRule).getFinishSecondaryWithPrimary();
        }
        return SplitRule.FINISH_NEVER;
    }

    static boolean isStickyPlaceholderRule(@NonNull SplitRule splitRule) {
        if (!(splitRule instanceof SplitPlaceholderRule)) {
            return false;
        }
        return ((SplitPlaceholderRule) splitRule).isSticky();
    }

    @Override
    public String toString() {
        return "SplitContainer{"
                + " primaryContainer=" + mPrimaryContainer
                + " secondaryContainer=" + mSecondaryContainer
                + " splitRule=" + mSplitRule
                + " splitAttributes" + mSplitAttributes
                + "}";
    }
}
