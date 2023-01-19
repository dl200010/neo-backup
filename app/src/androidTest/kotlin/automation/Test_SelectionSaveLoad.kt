package automation

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import com.machiav3lli.backup.activities.MainActivityX
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

fun ComposeContentTestRule.waitUntilNodeCount(
    matcher: SemanticsMatcher,
    count: Int,
    timeoutMillis: Long = 1_000L
) {
    this.waitUntil(timeoutMillis) {
        this.onAllNodes(matcher).fetchSemanticsNodes().size == count
    }
}

fun ComposeContentTestRule.waitUntilExists(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 1_000L
) {
    return this.waitUntilNodeCount(matcher, 1, timeoutMillis)
}

fun ComposeContentTestRule.waitUntilDoesNotExist(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 1_000L
) {
    return this.waitUntilNodeCount(matcher, 0, timeoutMillis)
}

fun ComposeContentTestRule.onNodeWait(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 1_000L
): SemanticsNodeInteraction? {
    var node: SemanticsNodeInteraction? = null
    try {
        this.waitUntil(timeoutMillis) {
            val nodes = this.onAllNodes(matcher)
            if(nodes.fetchSemanticsNodes().size > 0) {
                node = nodes.onFirst()
                true
            } else
                false
        }
    } catch(e: ComposeTimeoutException) {
        Timber.d("----------", "Timeout onNodeWait($matcher, $timeoutMillis)")
        return null
    }
    return node
}

fun ComposeContentTestRule.onNodeWaitOrAssert(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 1_000L,
    assert: Boolean = false
): SemanticsNodeInteraction {
    val node = onNodeWait(matcher, timeoutMillis)
    return node ?: throw AssertionError("node with (${matcher.description}) does not exist")
}

class Test_SelectionSaveLoad {

    @Rule
    @JvmField
    var test: ComposeContentTestRule = createAndroidComposeRule<MainActivityX>()

    @Before
    fun setUp() {
        //test.setContent {  }
        test.onRoot().printToLog("root")
    }

    @Test
    fun test_findList() {
        test.waitForIdle()
        val column = test.onNodeWait(hasTestTag("VerticalItemList.Column"), 10000)
        column?.printToLog("column") ?: Timber.d("----------", "ERROR")
        assert(column != null)
        column?.let {
            // select 1. item by switching to selection mode, then select 2. and 3.
            it.onChildAt(0).performTouchInput { longClick(center) }
            test.waitForIdle()
            it.onChildAt(1).performTouchInput { click(center) }
            test.waitForIdle()
            it.onChildAt(2).performTouchInput { click(center) }
            test.waitForIdle()
            // open context menu
            it.onChildAt(0).performTouchInput { longClick(center) }
            test.waitForIdle()

            val selectionName = "selection-${System.currentTimeMillis()}"

            // save selection as "selection-XXX"
            test.onNodeWaitOrAssert(hasText("Save")).performTouchInput { click(center) }
            test.waitForIdle()
            test.onNodeWithTag("input").assertIsFocused()
            test.onNodeWithTag("input").performTextInput("$selectionName\n")
            test.waitForIdle()

            // open menu again
            it.onChildAt(0).performTouchInput { longClick(center) }
            test.waitForIdle()
            it.onChildAt(0).performTouchInput { longClick(center) }
            test.waitForIdle()
            // open sub-menu "Load"
            test.onNodeWaitOrAssert(hasText("Load")).performTouchInput { click(center) }
            test.waitForIdle()
            // count menu items
            val count = test.onAllNodesWithText(selectionName).fetchSemanticsNodes().size
            assertEquals("menu entries", count, 1)
        }
    }
}