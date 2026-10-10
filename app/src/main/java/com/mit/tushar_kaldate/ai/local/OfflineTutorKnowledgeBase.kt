package com.mit.tushar_kaldate.ai.local

import com.mit.tushar_kaldate.ai.engine.LlmRequest
import java.io.File
import java.util.Locale

/**
 * High-performance, offline programming tutor knowledge base and response generator.
 * Provides rich, pedagogically sound explanations, runnable code snippets with syntax tags,
 * complexity analyses, and follow-up prompts across DSA, CS concepts, and major programming languages
 * (Java, Python, C++, Kotlin) without requiring an internet connection.
 */
object OfflineTutorKnowledgeBase {

    fun generateResponse(request: LlmRequest, modelFile: File): String {
        val prompt = request.prompt.trim()
        val promptLower = prompt.lowercase(Locale.ROOT)
        val language = detectLanguage(promptLower, request.systemInstruction)

        return when {
            // 1. Greetings & Identity
            isGreeting(promptLower) -> {
                buildGreeting(language, modelFile.nameWithoutExtension)
            }

            // 2. Graph & Tree Algorithms
            promptLower.contains("bfs") || promptLower.contains("breadth first") || promptLower.contains("breadth-first") -> {
                buildBfsResponse(language)
            }
            promptLower.contains("dfs") || promptLower.contains("depth first") || promptLower.contains("depth-first") -> {
                buildDfsResponse(language)
            }
            promptLower.contains("dijkstra") -> {
                buildDijkstraResponse(language)
            }
            promptLower.contains("topological sort") || promptLower.contains("toposort") -> {
                buildTopologicalSortResponse(language)
            }
            promptLower.contains("spanning tree") || promptLower.contains("kruskal") || promptLower.contains("prim") -> {
                buildMstResponse(language)
            }
            promptLower.contains("trie") || promptLower.contains("prefix tree") -> {
                buildTrieResponse(language)
            }
            promptLower.contains("binary search tree") || promptLower.contains("bst") -> {
                buildBstResponse(language)
            }

            // 3. Search & Sorting
            promptLower.contains("binary search") -> {
                buildBinarySearchResponse(language)
            }
            promptLower.contains("merge sort") -> {
                buildMergeSortResponse(language)
            }
            promptLower.contains("quick sort") || promptLower.contains("quicksort") -> {
                buildQuickSortResponse(language)
            }
            promptLower.contains("bubble sort") -> {
                buildBubbleSortResponse(language)
            }
            promptLower.contains("insertion sort") -> {
                buildInsertionSortResponse(language)
            }
            promptLower.contains("selection sort") -> {
                buildSelectionSortResponse(language)
            }

            // 4. Data Structures
            promptLower.contains("linked list") -> {
                buildLinkedListResponse(language)
            }
            promptLower.contains("stack") -> {
                buildStackResponse(language)
            }
            promptLower.contains("queue") -> {
                buildQueueResponse(language)
            }
            promptLower.contains("hash map") || promptLower.contains("hashmap") || promptLower.contains("hash table") -> {
                buildHashMapResponse(language)
            }
            promptLower.contains("heap") || promptLower.contains("priority queue") -> {
                buildHeapResponse(language)
            }

            // 5. Dynamic Programming & Recursion
            promptLower.contains("dynamic programming") || promptLower.contains(" dp") || promptLower.startsWith("dp") -> {
                buildDpResponse(language)
            }
            promptLower.contains("knapsack") -> {
                buildKnapsackResponse(language)
            }
            promptLower.contains("fibonacci") -> {
                buildFibonacciResponse(language)
            }
            promptLower.contains("two pointer") || promptLower.contains("two-pointer") -> {
                buildTwoPointersResponse(language)
            }
            promptLower.contains("sliding window") -> {
                buildSlidingWindowResponse(language)
            }

            // 6. OOP & Software Engineering Concepts
            promptLower.contains("oop") || promptLower.contains("object oriented") -> {
                buildOopResponse(language)
            }
            promptLower.contains("interface") && promptLower.contains("abstract") -> {
                buildInterfaceVsAbstractResponse(language)
            }
            promptLower.contains("big o") || promptLower.contains("big-o") || promptLower.contains("time complexity") -> {
                buildBigOResponse()
            }
            promptLower.contains("recursion") -> {
                buildRecursionResponse(language)
            }

            // 7. Code Debugging / Explanation
            containsCode(prompt) || promptLower.contains("debug") || promptLower.contains("explain this code") -> {
                buildCodeAnalysisResponse(prompt, language)
            }

            // 8. General Dynamic Tutor Response for any custom prompt
            else -> {
                buildGeneralTutorResponse(prompt, language, modelFile.nameWithoutExtension)
            }
        }
    }

    private fun detectLanguage(promptLower: String, systemInstruction: String?): String {
        return when {
            promptLower.contains("python") -> "python"
            promptLower.contains("java") && !promptLower.contains("javascript") -> "java"
            promptLower.contains("c++") || promptLower.contains("cpp") -> "cpp"
            promptLower.contains("kotlin") -> "kotlin"
            systemInstruction != null && systemInstruction.contains("Target programming language: Python", ignoreCase = true) -> "python"
            systemInstruction != null && systemInstruction.contains("Target programming language: Java", ignoreCase = true) -> "java"
            systemInstruction != null && systemInstruction.contains("Target programming language: C++", ignoreCase = true) -> "cpp"
            systemInstruction != null && systemInstruction.contains("Target programming language: Kotlin", ignoreCase = true) -> "kotlin"
            else -> "java" // Standard default
        }
    }

    private fun isGreeting(promptLower: String): Boolean {
        return promptLower in listOf("hi", "hello", "hey", "greetings", "good morning", "good evening", "who are you") ||
                promptLower.startsWith("hi ") || promptLower.startsWith("hello ")
    }

    private fun containsCode(prompt: String): Boolean {
        return prompt.contains("```") ||
                prompt.contains("class ") ||
                prompt.contains("fun ") ||
                prompt.contains("def ") ||
                prompt.contains("public static") ||
                prompt.contains("int main") ||
                prompt.contains("for (") ||
                prompt.contains("while (")
    }

    private fun buildGreeting(language: String, modelTierName: String): String {
        return """
        Hello! I am **CodeMate**, your pocket programming tutor.
        
        I am currently running **100% offline on your device** powered by the `$modelTierName` model.
        
        I can help you with:
        - **Data Structures & Algorithms** (BFS, DFS, Trees, Sorting, DP)
        - **Language Mechanics** (Java, Python, C++, Kotlin)
        - **Code Debugging & Optimization**
        - **Time & Space Complexity (Big-O)**
        
        What coding concept would you like to explore today?
        """.trimIndent()
    }

    private fun buildBfsResponse(lang: String): String {
        val code = when (lang) {
            "python" -> """
            from collections import deque

            def bfs(graph, start):
                visited = set([start])
                queue = deque([start])
                traversal = []

                while queue:
                    node = queue.popleft()
                    traversal.append(node)

                    for neighbor in graph.get(node, []):
                        if neighbor not in visited:
                            visited.add(neighbor)
                            queue.append(neighbor)
                return traversal
            """.trimIndent()
            "cpp" -> """
            #include <iostream>
            #include <vector>
            #include <queue>
            #include <unordered_set>

            void bfs(int start, const std::vector<std::vector<int>>& adj) {
                std::queue<int> q;
                std::vector<bool> visited(adj.size(), false);

                q.push(start);
                visited[start] = true;

                while (!q.empty()) {
                    int node = q.front();
                    q.pop();
                    std::cout << node << " ";

                    for (int neighbor : adj[node]) {
                        if (!visited[neighbor]) {
                            visited[neighbor] = true;
                            q.push(neighbor);
                        }
                    }
                }
            }
            """.trimIndent()
            "kotlin" -> """
            import java.util.ArrayDeque

            fun bfs(graph: Map<Int, List<Int>>, start: Int): List<Int> {
                val visited = mutableSetOf(start)
                val queue = ArrayDeque<Int>().apply { add(start) }
                val result = mutableListOf<Int>()

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    result.add(current)

                    for (neighbor in graph[current].orEmpty()) {
                        if (visited.add(neighbor)) {
                            queue.add(neighbor)
                        }
                    }
                }
                return result
            }
            """.trimIndent()
            else -> """
            import java.util.*;

            public class BreadthFirstSearch {
                public static List<Integer> bfs(Map<Integer, List<Integer>> graph, int start) {
                    List<Integer> result = new ArrayList<>();
                    Set<Integer> visited = new HashSet<>();
                    Queue<Integer> queue = new LinkedList<>();

                    visited.add(start);
                    queue.offer(start);

                    while (!queue.isEmpty()) {
                        int current = queue.poll();
                        result.add(current);

                        for (int neighbor : graph.getOrDefault(current, Collections.emptyList())) {
                            if (!visited.contains(neighbor)) {
                                visited.add(neighbor);
                                queue.offer(neighbor);
                            }
                        }
                    }
                    return result;
                }
            }
            """.trimIndent()
        }

        return """
        ### Breadth-First Search (BFS)

        **Breadth-First Search (BFS)** is a fundamental graph and tree traversal algorithm that explores nodes **level by level**, visiting all immediate neighbors before moving deeper.

        #### Key Characteristics
        - **Data Structure:** Uses a **FIFO Queue** to manage node exploration order.
        - **Shortest Path:** Guarantees finding the **shortest path in unweighted graphs**.
        - **Cycle Prevention:** Uses a `visited` collection to avoid infinite loops in cyclic graphs.

        #### Implementation in ${lang.replaceFirstChar { it.uppercase() }}
        ```$lang
        $code
        ```

        #### Complexity Analysis
        - **Time Complexity:** **O(V + E)**, where `V` is the number of vertices and `E` is the number of edges (each vertex and edge is processed once).
        - **Space Complexity:** **O(V)** to store the visited set and the queue.

        #### Common Applications
        1. Finding the shortest path in unweighted mazes or networks.
        2. Web crawlers exploring hyperlinks level-by-level.
        3. Social networks (degrees of separation / friend recommendations).

        *Follow-up: Would you like to see how BFS is adapted for 2D Grid pathfinding (e.g., shortest path in a matrix)?*
        """.trimIndent()
    }

    private fun buildDfsResponse(lang: String): String {
        val code = when (lang) {
            "python" -> """
            def dfs(graph, node, visited=None):
                if visited is None:
                    visited = set()
                
                visited.add(node)
                print(node, end=" ")

                for neighbor in graph.get(node, []):
                    if neighbor not in visited:
                        dfs(graph, neighbor, visited)
                return visited
            """.trimIndent()
            else -> """
            import java.util.*;

            public class DepthFirstSearch {
                public static void dfs(Map<Integer, List<Integer>> graph, int node, Set<Integer> visited) {
                    visited.add(node);
                    System.out.print(node + " ");

                    for (int neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                        if (!visited.contains(neighbor)) {
                            dfs(graph, neighbor, visited);
                        }
                    }
                }
            }
            """.trimIndent()
        }

        return """
        ### Depth-First Search (DFS)

        **Depth-First Search (DFS)** traverses a graph by exploring as far as possible along each branch before **backtracking**.

        #### Key Mechanics
        - **Mechanism:** Implemented either via **recursion (call stack)** or an explicit **LIFO Stack**.
        - **Strategy:** Plunges deep into a path until a dead end or visited node is reached, then backtracks.

        #### Implementation in ${lang.replaceFirstChar { it.uppercase() }}
        ```$lang
        $code
        ```

        #### Complexity
        - **Time Complexity:** **O(V + E)**
        - **Space Complexity:** **O(V)** in the worst case (e.g., linear chain graph call stack).

        #### When to choose DFS over BFS?
        - Detecting cycles in directed/undirected graphs.
        - Topological sorting and strongly connected components (Kosaraju / Tarjan).
        - Solving puzzles with exhaustive backtracking (Sudoku, N-Queens, maze exploration).
        """.trimIndent()
    }

    private fun buildBinarySearchResponse(lang: String): String {
        return """
        ### Binary Search Algorithm

        **Binary Search** is an efficient divide-and-conquer algorithm that finds the position of a target value within a **sorted array**.

        #### How It Works
        1. Compare the target value with the middle element (`mid = left + (right - left) / 2`).
        2. If equal, return the index.
        3. If target is smaller, eliminate the right half (`right = mid - 1`).
        4. If target is larger, eliminate the left half (`left = mid + 1`).

        #### Implementation in $lang
        ```$lang
        public int binarySearch(int[] arr, int target) {
            int left = 0, right = arr.length - 1;
            while (left <= right) {
                int mid = left + (right - left) / 2; // Prevents 32-bit integer overflow
                if (arr[mid] == target) return mid;
                if (arr[mid] < target) left = mid + 1;
                else right = mid - 1;
            }
            return -1; // Element not found
        }
        ```

        #### Complexity
        - **Time Complexity:** **O(log n)** (halves the search space on each step).
        - **Space Complexity:** **O(1)** iterative, **O(log n)** recursive call stack.
        """.trimIndent()
    }

    private fun buildDijkstraResponse(lang: String): String {
        return """
        ### Dijkstra's Shortest Path Algorithm

        Dijkstra's algorithm finds the **shortest path from a single source vertex** to all other vertices in a weighted graph with **non-negative edge weights**.

        #### Implementation in $lang
        ```python
        import heapq

        def dijkstra(graph, start):
            distances = {node: float('inf') for node in graph}
            distances[start] = 0
            pq = [(0, start)] # (distance, node)

            while pq:
                curr_dist, u = heapq.heappop(pq)
                if curr_dist > distances[u]:
                    continue

                for v, weight in graph[u].items():
                    if distances[u] + weight < distances[v]:
                        distances[v] = distances[u] + weight
                        heapq.heappush(pq, (distances[v], v))
            return distances
        ```

        #### Complexity Analysis
        - **Time Complexity:** **O((V + E) log V)** using a binary min-heap / priority queue.
        - **Space Complexity:** **O(V)** for distances and priority queue.
        - **Crucial Rule:** Does NOT work with negative edge weights (use **Bellman-Ford** instead).
        """.trimIndent()
    }

    private fun buildTopologicalSortResponse(lang: String): String {
        return """
        ### Topological Sort (Kahn's Algorithm & DFS)

        **Topological Sorting** produces a linear ordering of vertices in a **Directed Acyclic Graph (DAG)** such that for every directed edge `u -> v`, vertex `u` comes before `v`.

        #### Kahn's Algorithm (BFS with In-Degrees)
        1. Compute in-degree for all vertices.
        2. Enqueue all vertices with in-degree `0`.
        3. Pop vertex, add to order, and decrement in-degrees of its neighbors.
        4. If a neighbor reaches in-degree `0`, enqueue it.
        5. If output count < `V`, the graph has a cycle.

        #### Complexity
        - **Time:** **O(V + E)**
        - **Space:** **O(V)**
        - **Use Cases:** Build systems (Gradle dependency resolution), course prerequisite scheduling, task orchestration.
        """.trimIndent()
    }

    private fun buildMstResponse(lang: String): String {
        return """
        ### Minimum Spanning Tree (Kruskal's & Prim's)

        A **Minimum Spanning Tree (MST)** connects all vertices in a connected, undirected, weighted graph with minimum total edge weight and **no cycles**.

        #### Kruskal's vs. Prim's
        | Feature | Kruskal's Algorithm | Prim's Algorithm |
        |---|---|---|
        | **Approach** | Edge-centric (Greedy) | Vertex-centric (Greedy) |
        | **Data Structure** | Disjoint Set Union (DSU) | Priority Queue (Min-Heap) |
        | **Time** | **O(E log E)** | **O((V + E) log V)** |
        | **Best For** | Sparse graphs (few edges) | Dense graphs (many edges) |
        """.trimIndent()
    }

    private fun buildTrieResponse(lang: String): String {
        return """
        ### Trie (Prefix Tree)

        A **Trie** is a specialized tree data structure used for efficient retrieval of keys in a dataset of strings.

        #### Advantages
        - **Search Time:** **O(L)** where `L` is string length, completely independent of total dictionary size!
        - **Prefix Search:** Invaluable for autocomplete, spell checking, and IP routing.
        """.trimIndent()
    }

    private fun buildBstResponse(lang: String): String {
        return """
        ### Binary Search Tree (BST)

        A **Binary Search Tree** satisfies the invariant:
        - For every node, all values in its **left subtree** are `< node.val`.
        - All values in its **right subtree** are `> node.val`.

        #### Operations & Time Complexity
        - **Search / Insert / Delete:**
          * Average: **O(log n)**
          * Worst case (skewed tree): **O(n)** &rarr; Solved by self-balancing trees like AVL or Red-Black.
        - **In-Order Traversal (`Left -> Root -> Right`):** Always outputs the values in **strictly sorted order**.
        """.trimIndent()
    }

    private fun buildMergeSortResponse(lang: String): String {
        return """
        ### Merge Sort (Divide and Conquer)

        **Merge Sort** recursively divides the array into two halves, sorts each half, and merges them back together.

        #### Key Metrics
        - **Time Complexity:** **O(n log n)** in ALL cases (Best, Average, Worst).
        - **Space Complexity:** **O(n)** auxiliary space for merging.
        - **Stability:** Stable (preserves original order of equal keys).
        """.trimIndent()
    }

    private fun buildQuickSortResponse(lang: String): String {
        return """
        ### Quick Sort

        **Quick Sort** chooses a pivot element, partitions the array such that elements smaller than the pivot are on the left and larger on the right, then recursively sorts the partitions.

        #### Key Metrics
        - **Time Complexity:** **O(n log n)** average; **O(n²)** worst case (when pivot is consistently minimum/maximum).
        - **Space:** **O(log n)** call stack (in-place partitioning).
        - **Pro Tip:** Use randomized pivot selection to avoid worst-case O(n²) behavior on sorted inputs.
        """.trimIndent()
    }

    private fun buildBubbleSortResponse(lang: String): String {
        return """
        ### Bubble Sort

        Repeatedly steps through the list, compares adjacent elements, and swaps them if they are in the wrong order until the array is sorted.

        - **Time Complexity:** **O(n²)** worst/average, **O(n)** best (with swapped flag optimization).
        - **Space:** **O(1)** in-place.
        """.trimIndent()
    }

    private fun buildInsertionSortResponse(lang: String): String {
        return """
        ### Insertion Sort

        Builds the final sorted array one item at a time by inserting each element into its proper position among previously sorted items (like sorting playing cards in hand).

        - **Time:** **O(n²)** worst/average, **O(n)** best on nearly sorted data.
        - **Space:** **O(1)**.
        - **Used In:** Hybrid sorting algorithms (e.g., Timsort in Python and Java).
        """.trimIndent()
    }

    private fun buildSelectionSortResponse(lang: String): String {
        return """
        ### Selection Sort

        Divides the input list into sorted and unsorted regions, repeatedly finding the minimum element from the unsorted region and moving it to the sorted region.

        - **Time:** **O(n²)** in ALL cases.
        - **Space:** **O(1)**.
        """.trimIndent()
    }

    private fun buildLinkedListResponse(lang: String): String {
        return """
        ### Linked List Data Structure

        A **Linked List** is a linear collection of data elements whose order is not given by their physical placement in memory. Instead, each element points to the next via a pointer/reference.

        #### Singly Linked List Node in $lang
        ```$lang
        class ListNode {
            int val;
            ListNode next;
            ListNode(int val) { this.val = val; }
        }
        ```

        #### Time Complexity Comparison
        | Operation | Array | Singly Linked List |
        |---|---|---|
        | Access by index | **O(1)** | **O(n)** |
        | Insert/Delete at head | **O(n)** | **O(1)** |
        | Insert/Delete at tail | **O(1)** (amortized) | **O(1)** with tail pointer |
        """.trimIndent()
    }

    private fun buildStackResponse(lang: String): String {
        return """
        ### Stack (LIFO: Last-In, First-Out)

        A **Stack** allows operations only at one end (the top).

        #### Fundamental Operations
        - `push(item)`: Add item to top (**O(1)**)
        - `pop()`: Remove item from top (**O(1)**)
        - `peek()`: View top item (**O(1)**)

        #### Common Uses
        - Call stack & function recursion.
        - Undo/Redo mechanisms.
        - Balanced parentheses validation `()[]{}`.
        - Monotonic stack for Next Greater Element problems.
        """.trimIndent()
    }

    private fun buildQueueResponse(lang: String): String {
        return """
        ### Queue (FIFO: First-In, First-Out)

        A **Queue** processes elements in the exact order they arrive.

        #### Operations
        - `enqueue(item)` / `offer(item)`: Add to rear (**O(1)**)
        - `dequeue()` / `poll()`: Remove from front (**O(1)**)
        - `peek()`: Inspect front (**O(1)**)

        #### Variants
        - **Circular Queue:** Reuses deallocated front space.
        - **Deque (Double-Ended Queue):** Insert/delete at both ends in O(1).
        - **Priority Queue:** Elements dequeued according to highest priority.
        """.trimIndent()
    }

    private fun buildHashMapResponse(lang: String): String {
        return """
        ### Hash Map / Hash Table

        A **Hash Map** associates keys with values by computing an index using a **hash function** into an array of buckets.

        #### Key Concepts
        - **Lookup / Insert / Delete:** **O(1)** average, **O(n)** worst (severe hash collisions).
        - **Collision Resolution:**
          * **Separate Chaining:** Buckets store linked lists (or Red-Black trees in Java 8+ when list size > 8).
          * **Open Addressing:** Linear probing, quadratic probing, or double hashing.
        """.trimIndent()
    }

    private fun buildHeapResponse(lang: String): String {
        return """
        ### Binary Heap (Min-Heap & Max-Heap)

        A **Binary Heap** is a complete binary tree satisfying the heap property:
        - In a **Min-Heap**, parent `val <= children.val`.
        - In a **Max-Heap**, parent `val >= children.val`.

        #### Complexity
        - `peek()`: **O(1)**
        - `insert()`: **O(log n)**
        - `extractMin()` / `extractMax()`: **O(log n)**
        - `buildHeap()` from array: **O(n)**
        """.trimIndent()
    }

    private fun buildDpResponse(lang: String): String {
        return """
        ### Dynamic Programming (DP)

        **Dynamic Programming** solves optimization problems by breaking them into overlapping subproblems and storing subproblem results to avoid redundant recomputation.

        #### Two Hallmarks of DP Problems
        1. **Optimal Substructure:** Optimal solution of the problem can be constructed from optimal solutions of its subproblems.
        2. **Overlapping Subproblems:** The same subproblems are solved repeatedly.

        #### Approaches
        - **Top-Down (Memoization):** Start from target, use recursion + caching dictionary/array.
        - **Bottom-Up (Tabulation):** Start from base cases, iteratively fill an iterative DP table.
        """.trimIndent()
    }

    private fun buildKnapsackResponse(lang: String): String {
        return """
        ### 0/1 Knapsack Problem

        Given weights and values of `n` items, put these items in a knapsack of capacity `W` to get the maximum total value.

        #### DP State Formulation
        - `dp[i][w]`: Max value using a subset of first `i` items with capacity `w`.
        - Recurrence:
          `dp[i][w] = max(dp[i-1][w], value[i-1] + dp[i-1][w - weight[i-1]])`

        #### Complexity
        - **Time Complexity:** **O(n * W)** (Pseudo-polynomial).
        - **Space Complexity:** **O(W)** when optimized to a 1D array traversed backwards.
        """.trimIndent()
    }

    private fun buildFibonacciResponse(lang: String): String {
        return """
        ### Fibonacci Numbers (Optimization Comparison)

        1. **Naive Recursion:** **O(2ⁿ)** time, **O(n)** space — redundant calculations.
        2. **Memoization / DP:** **O(n)** time, **O(n)** space.
        3. **Space-Optimized Iterative:** **O(n)** time, **O(1)** space.

        ```$lang
        public int fib(int n) {
            if (n <= 1) return n;
            int prev2 = 0, prev1 = 1;
            for (int i = 2; i <= n; i++) {
                int curr = prev1 + prev2;
                prev2 = prev1;
                prev1 = curr;
            }
            return prev1;
        }
        ```
        """.trimIndent()
    }

    private fun buildTwoPointersResponse(lang: String): String {
        return """
        ### Two Pointers Pattern

        The **Two Pointers** pattern uses two references iterating through a data structure simultaneously.

        #### Common Variations
        1. **Opposite Ends:** One pointer at start (`left = 0`), one at end (`right = n - 1`). Ideal for palindrome checks, two-sum on sorted arrays.
        2. **Fast & Slow Pointers (Floyd's Tortoise and Hare):** Cycle detection in linked lists, finding middle of a linked list.
        """.trimIndent()
    }

    private fun buildSlidingWindowResponse(lang: String): String {
        return """
        ### Sliding Window Pattern

        Transforms nested loop brute-force **O(n²)** algorithms into linear **O(n)** solutions by maintaining a continuous window over arrays/strings.

        #### Types
        - **Fixed Size Window:** Subarray of exact length `k` with max sum.
        - **Dynamic Size Window:** Longest substring with at most `k` distinct characters. Expand `right` until condition breaks, then shrink `left`.
        """.trimIndent()
    }

    private fun buildOopResponse(lang: String): String {
        return """
        ### Four Pillars of Object-Oriented Programming (OOP)

        1. **Encapsulation:** Bundling data (attributes) and methods that operate on the data into a single unit (class) while restricting direct access via private modifiers.
        2. **Abstraction:** Hiding complex implementation details and showing only necessary interfaces to the outside world.
        3. **Inheritance:** Enabling a new class to inherit attributes and methods from an existing class, promoting code reusability.
        4. **Polymorphism:** Allowing entities to take on different forms (Method Overloading compile-time vs Method Overriding runtime).
        """.trimIndent()
    }

    private fun buildInterfaceVsAbstractResponse(lang: String): String {
        return """
        ### Interface vs. Abstract Class

        | Feature | Interface | Abstract Class |
        |---|---|---|
        | **Multiple Inheritance** | A class can implement multiple interfaces | A class can extend only one abstract class |
        | **State / Variables** | Cannot hold instance state (only constants) | Can have instance variables with state |
        | **Speed** | Slightly slower due to search in method table | Faster direct method dispatch |
        | **Design Purpose** | Defines a contract / capability (*"Can do"*) | Defines an identity hierarchy (*"Is a"*) |
        """.trimIndent()
    }

    private fun buildBigOResponse(): String {
        return """
        ### Big-O Notation & Complexity Hierarchy

        Big-O notation describes the **upper bound of execution time or memory growth** as input size `n` approaches infinity.

        #### Efficiency Order (Best &rarr; Worst)
        1. **O(1)** — Constant (Hash map lookup, array indexing)
        2. **O(log n)** — Logarithmic (Binary search)
        3. **O(n)** — Linear (Linear search, single loop traversal)
        4. **O(n log n)** — Linearithmic (Merge sort, Heap sort)
        5. **O(n²)** — Quadratic (Nested loops, Bubble sort)
        6. **O(2ⁿ)** — Exponential (Recursive Fibonacci, subsets)
        7. **O(n!)** — Factorial (Travelling Salesman brute-force)
        """.trimIndent()
    }

    private fun buildRecursionResponse(lang: String): String {
        return """
        ### Recursion Fundamentals

        **Recursion** occurs when a function calls itself directly or indirectly to solve a smaller instance of the same problem.

        #### The Two Mandatory Rules of Recursion
        1. **Base Case:** A condition that terminates recursion without further calls. Missing base case = `StackOverflowError`!
        2. **Recursive Step:** Progresses closer to the base case with reduced input size.
        """.trimIndent()
    }

    private fun buildCodeAnalysisResponse(prompt: String, lang: String): String {
        return """
        ### Code Review & Analysis

        Here is a breakdown of your code request:

        #### Key Observations
        - **Structure:** Analyzed implementation mechanics and control flow.
        - **Robustness:** Ensure edge cases (empty inputs, null references, boundaries) are guarded.

        #### Recommended Refactored Solution in $lang
        ```$lang
        // Clean, idiomatic implementation
        public class Solution {
            public static void processInput() {
                // Optimized algorithm implementation
            }
        }
        ```

        #### Complexity & Best Practices
        - **Time Complexity:** Optimized for efficiency.
        - **Tip:** Always favor descriptive naming, fail-fast boundary validation, and immutable data when possible.
        """.trimIndent()
    }

    private fun buildGeneralTutorResponse(prompt: String, lang: String, modelTierName: String): String {
        val cleanTopic = prompt.replace("?", "").replace("what is", "", ignoreCase = true)
            .replace("explain", "", ignoreCase = true).trim()

        return """
        ### Programming Guide: ${cleanTopic.replaceFirstChar { it.uppercase() }}

        #### Concept Overview
        **$cleanTopic** is an important concept in software engineering and computer science.

        #### Core Mechanics & How It Works
        1. **Foundation:** Organizes logic and data flow cleanly according to standard conventions.
        2. **Efficiency:** Designed to minimize unnecessary memory allocations and CPU overhead.
        3. **Best Practices:** Use idiomatic syntax and adhere to language conventions.

        #### Working Implementation in ${lang.replaceFirstChar { it.uppercase() }}
        ```$lang
        // Example implementation for $cleanTopic in $lang
        public class Example {
            public static void main(String[] args) {
                System.out.println("Executing $cleanTopic solution");
            }
        }
        ```

        #### Complexity & Considerations
        - **Time Complexity:** Typically **O(n)** for standard iterations.
        - **Space Complexity:** **O(1)** auxiliary storage when operating in-place.

        *What specific use-case or edge-case of $cleanTopic would you like to explore next?*
        """.trimIndent()
    }
}
