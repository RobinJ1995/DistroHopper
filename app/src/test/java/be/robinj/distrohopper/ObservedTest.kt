package be.robinj.distrohopper

import org.junit.Assert.assertEquals
import org.junit.Test

class ObservedTest {
    private class Subject : Observed() {
        fun notifyObservers() = nudgeObservers()
    }

    @Test fun attachedObserverIsNotified() {
        val subject = Subject(); var calls = 0
        subject.attachObserver { calls++ }
        subject.notifyObservers()
        assertEquals(1, calls)
    }

    @Test fun detachedObserverIsNotNotified() {
        val subject = Subject(); var calls = 0
        val observer = IObserver { calls++ }
        subject.attachObserver(observer); subject.detachObserver(observer); subject.notifyObservers()
        assertEquals(0, calls)
    }

    @Test fun multipleObserversAreNotified() {
        val subject = Subject(); var first = 0; var second = 0
        subject.attachObserver { first++ }; subject.attachObserver { second++ }; subject.notifyObservers()
        assertEquals(1, first); assertEquals(1, second)
    }

    // The dev log nudges from background threads while observers attach and detach on
    // the main one; detaching mid-nudge is the same hazard in miniature. //
    @Test fun observerCanDetachItselfWhileBeingNudged() {
        val subject = Subject(); var calls = 0
        val observer = object : IObserver {
            override fun nudge() { calls++; subject.detachObserver(this) }
        }
        subject.attachObserver(observer)
        subject.notifyObservers(); subject.notifyObservers()
        assertEquals(1, calls)
    }

    @Test fun observerIsNotifiedOnEveryNudge() {
        val subject = Subject(); var calls = 0
        subject.attachObserver { calls++ }; subject.notifyObservers(); subject.notifyObservers()
        assertEquals(2, calls)
    }
}
