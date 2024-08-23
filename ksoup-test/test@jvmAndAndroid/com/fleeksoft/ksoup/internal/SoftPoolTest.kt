package com.fleeksoft.ksoup.internal

import kotlin.test.*

class SoftPoolTest {
    @Test
    fun testSoftLocalPool() {
        val softLocalPool: SoftPool<CharArray> = SoftPool { CharArray(BufSize) }

        val executorService: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newFixedThreadPool(NumThreads)
        val latch: java.util.concurrent.CountDownLatch = java.util.concurrent.CountDownLatch(NumThreads)

        fun getNThreads(): List<HashSet<CharArray>> {
            return buildList {
                repeat(NumThreads) {
                    add(HashSet())
                }
            }
        }

        val allBuffers: MutableSet<CharArray> = java.util.HashSet<CharArray>()
        val threadLocalBuffers: Array<MutableSet<CharArray>> = arrayOf(*getNThreads().toTypedArray())

        val threadCount: java.util.concurrent.atomic.AtomicInteger = java.util.concurrent.atomic.AtomicInteger()

        val task = Runnable {
            try {
                val threadIndex: Int = threadCount.getAndIncrement()
                val localBuffers: MutableSet<CharArray> = java.util.HashSet<CharArray>()
                // First borrow
                for (i in 0 until NumObjects) {
                    val buffer = softLocalPool.borrow()
                    assertEquals(BufSize, buffer.size)
                    localBuffers.add(buffer)
                }

                // Release buffers back to the pool
                for (buffer in localBuffers) {
                    softLocalPool.release(buffer)
                }

                // Borrow again and ensure buffers are reused
                for (i in 0 until NumObjects) {
                    val buffer = softLocalPool.borrow()
                    assertTrue(localBuffers.contains(buffer), "Buffer was not reused in the same thread")
                    threadLocalBuffers[threadIndex].add(buffer)
                }

                synchronized(allBuffers) {
                    allBuffers.addAll(threadLocalBuffers[threadIndex])
                }
            } finally {
                latch.countDown()
            }
        }

        // Run the tasks
        for (i in 0 until NumThreads) {
            executorService.submit { task.run() }
        }

        // Wait for all threads to complete
        latch.await()
        executorService.shutdown()

        // Ensure no buffers are shared between threads
        val uniqueBuffers: MutableSet<CharArray> = java.util.HashSet<CharArray>()
        for (bufferSet in threadLocalBuffers) {
            for (buffer in bufferSet) {
                assertTrue(uniqueBuffers.add(buffer), "Buffer was shared between threads")
            }
        }
    }

    @Test
    fun testSoftReferenceBehavior() {
        val softLocalPool: SoftPool<CharArray> = SoftPool { CharArray(BufSize) }

        // Borrow and release an object
        val buffer = softLocalPool.borrow()
        assertEquals(BufSize, buffer.size)
        softLocalPool.release(buffer)

        // Fake a GC
        softLocalPool.threadLocalStack.get().clear()

        // Ensure the object is garbage collected
//        assertNull(softLocalPool.threadLocalStack.get()) // IN KMP we don't have weak reference

        val second = softLocalPool.borrow()
        // should be different, but same size
        assertNotEquals(buffer, second)
        assertEquals(BufSize, second.size)
    }

    @Test
    fun testBorrowFromEmptyPool() {
        val softLocalPool: SoftPool<CharArray> = SoftPool { CharArray(BufSize) }

        // Borrow from an empty pool
        val buffer = softLocalPool.borrow()
        assertNotNull(buffer, "Borrowed null from an empty pool")
        assertEquals(BufSize, buffer.size)
    }

    @Test
    fun testReleaseMoreThanMaxIdle() {
        val softLocalPool: SoftPool<CharArray> = SoftPool { CharArray(BufSize) }

        // Borrow more than MaxIdle objects
        val borrowedBuffers: MutableList<CharArray> = java.util.ArrayList<CharArray>()
        for (i in 0 until SoftPool.MaxIdle + 5) {
            val buffer = softLocalPool.borrow()
            borrowedBuffers.add(buffer)
        }

        // Release all borrowed objects back to the pool
        for (buffer in borrowedBuffers) {
            softLocalPool.release(buffer)
        }

        // Ensure the pool size does not exceed MaxIdle
        val stack = softLocalPool.stack
        assertTrue(stack.size <= SoftPool.MaxIdle, "Pool size exceeded MaxIdle limit")
    }

    companion object {
        private const val BufSize = 12
        private const val NumThreads = 5
        private const val NumObjects = 3
    }
}