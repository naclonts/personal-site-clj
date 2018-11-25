# Breadth First Search Algorithm in Clojure

Below is an implementation of a graph structure and breadth-first search (BFS) algorithm that allows each vertex or node to contain any payload you like -- a map, a string, whatever.

1. [Why a graph algorithm?](#why_a_dang_graph_algorithm?)
2. [The Graph and the Vertex](#the_graph_and_the_vertex)
3. [Fundamental Operations](#fundamental_operations)
4. [The Search they call Breadth First](#the_search_they_call_breadth_first)

## Why a dang graph algorithm?
Graph traversal algorithms like Breadth-First Search and its close cousin, Depth-First Search, open up fast solutions to some interesting problems including:

* [Graph coloring](https://en.wikipedia.org/wiki/Graph_coloring), used in drawing maps, creating compilers, and playing Sudoku
* Finding **articulation vertices**, which are likely points of failure in graphs such as telephone networks, internet networks, and more
* Making your way out of a maze by the shortest path possible

After starting a new job working with Python and JavaScript, I felt the itch to learn something in the functional tradition. Eventually I settled on Clojure, a dialect of the venerable Lisp. Working with algorithms and data structures is one of the best ways to learn a new language, so I translated a breadth-first search algorithm into Clojure, and in the process found some interesting differences between the Lispey code and the classic imperative style.

Much credit is due to the free book [Problem Solving with Algorithms and Data Structures using Python](http://interactivepython.org/runestone/static/pythonds/index.html), which provided the basis for the graph data structure implementation, and [*The Algorithm Design Manual*](http://www.algorist.com) by Steven Skiena, which inspired the BFS algorithm used here. I recommend both to anyone who'd like to beef up their algorithmic chops.

## The Graph and the Vertex
First off, let's define our data structures. Each Vertex has a key, a value, and a list of connected vertices. Each Graph is just a collection of vertices.

[[ insert basic graph drawing ]]
```clojure
(defrecord Graph [vertices])
(defrecord Vertex
	[key value connections parent])
```

The `Graph` type contains just one morsel of data:
* `vertices`: a Clojure map object, where each key is a `Vertex` object's key, and each value is the `Vertex` object itself.

The `Vertex` has a few more attributes:
* `key`: a unique identifier representing this vertex
* `value`: an arbitrary payload you'd like to associate with the vertex
* `connections`: a set of keys to vertices that are connected to this node (often called *edges* or *links*)
* `parent`: the key to this vertex's parent, to be used in the BFS implementation later

## Fundamental operations
We'll use a few basic operations to experiment with our new structure:

* **Add** a new vertex to the graph
* **Get** an existing vertex from the graph
* **Create an edge**, or connection, between two existing vertices
* **Check if two vertices are connected**

We'll implement each of these in a *purely functional* way, meaning the function performing each operation has no side effects (it just returns a new output based on the input) and the function's return value is always the same for the same inputs. This is a powerful way to program, drawing deep from the mighty roots of mathematics, and luckily Clojure makes it natural to work this way!

```clojure
(defn add-vertex
	"Add a new vertex with the given `key` and `value` to a `graph`."
	[{:keys [vertices] :as graph} key value]
	(->Graph (assoc
		        vertices
		        key
		        (->Vertex key value #{} nil))))

(defn get-vertex
	[{:keys [vertices] :as graph} key]
	(get vertices key))

(defn add-edge-directed
	"Connects vertex `u` to `v` with a one-way / directed edge."
	[u v]
	(assoc u :connections (conj (:connections u) (:key v))))

(defn add-edge
	"Creates an undirected edge between vertices `u` and `v`."
	[{:keys [vertices] :as graph} u v]
	(->Graph
	 ;; Notice the thread "->" operation here, which inserts `vertices`
   ;; as the first argument of the two `assoc` calls
	 (-> vertices
		   (assoc (:key u) (add-edge-directed u v))
		   (assoc (:key v) (add-edge-directed v u)))))

(defn connected?
	"Returns `true` if `u` has an edge to `v`; otherwise, returns `false`."
	[u v]
	(contains? (:connections u) (:key v)))
```

A couple of things to note here:

1. Each function that "modifies" the graph (`add-vertex`, `add-edge-directed`, and `add-edge`) returns a *new Graph object*, rather than editing the existing object. In terms of writing pure functions, this is an example of "no side effects".
2. Our `add-edge` function returns an *undirected* edge, meaning that if `u` is connected to `v`, then `v` is also connected to `u`. Depending on your problem, you might want to instead use `add-edge-directed` to make a one-way connection.

[[insert illustration of directed vs undirected here]]

In addition to the operations above, we'll add a couple of utility functions to print our graph:
```clojure
;; These functions are, to use a word Nathaniel Hawthorne's grandfather would
;; use to describe our modern ways, "impure" because they have the side effect
;; of printing to the screen.
;; It turns out that all useful things are, indeed, impure.
(defn print-vertex [v]
	(println (str
				    "\t" (:key v)
				    "\t- conn: " (:connections v)
				    "\t- parent: " (:parent v))))

(defn print-graph [{:keys [vertices] :as graph}]
	(println "Graph:")
	(doseq [v (map val vertices)]
		(print-vertex v))
  graph)
```

Now for the fun part: testing our new structure and seeing what it looks like!

Here we'll use the handy threading macro `as->` to progressively transform our Graph object.  Each call to `add-vertex/edge` returns a new Graph, which the `as->` macro assigns to the name `g` for use in the next line.

```clojure
(as-> (->Graph {}) g
  (add-vertex g "A" 1)
  (add-vertex g "B" 2)
  (add-edge g (get-vertex g "A") (get-vertex g "B"))
  (print-graph g))
;; -- Output --
;; Graph:
;;	A	- conn: #{"B"}	- parent: 
;;	B	- conn: #{"A"}	- parent:

(as-> (->Graph {}) g
  (add-vertex g "Answer" 42)
  (add-vertex g "Question" "Meaning?")
  (add-vertex g "Thanks" "Fish")
  (add-vertex g "Bob" {:occupation :painting-genius})
  (add-edge g (get-vertex g "Answer") (get-vertex g "Question"))
  (add-edge g (get-vertex g "Answer") (get-vertex g "Thanks"))
  (print-graph g)
  (println
   "Are Answer and Question connected? "
   (connected? (get-vertex g "Answer") (get-vertex g "Question"))))
;; -- Output --
;; Graph:
;; 	Answer	- conn: #{"Question" "Thanks"}	- parent: 
;; 	Question	- conn: #{"Answer"}	- parent: 
;; 	Thanks	- conn: #{"Answer"}	- parent: 
;; 	Bob	- conn: #{}	- parent: 
;; Are Answer and Question connected?  true
```

## The Search they call Breadth First
Now for the actual Breadth-First Search algorithm! In pseudo-code, the algorithm can be described like this:

```text
BFS(graph, start-node)
  * Mark all vertices in `graph` as "undiscovered"
  * Mark `start-node` as "discovered"
  * Create a FIFO (first-in first-out) queue `Q` initially containing `start-node`
  * While `Q` is not empty:
    * Pop the first element from `Q`, assign to `u`
    * Process vertex `u` as needed
    * For each vertex `v` adjacent to `u`:
      * Process edge `(u, v)` as needed
      * If `v` is "undiscovered":
        * Mark `v` as "discovered"
        * Set `v`'s parent to `u`
        * Add `v` to `Q`
      * Mark `u` as "completely explored"
```

This is in a somewhat imperative style, so we'll make some adjustments to utilize the *Power of the Lambda*.

Our breadth first search function will accept a graph object and a starting vertex. It will return the graph with each vertex's `:parent` property updated to the appropriate parent's key for a BFS path.

```clojure
;; For ClojureScript programs, replace both instances of
;; `clojure.lang.PersistentQueue/EMPTY` below with `#queue []`
(use '[clojure.string :only (join)])

(defn bfs
	"Return `graph` with each vertex's `:parent` property updated."
	[graph start]
	(loop [discovered clojure.lang.PersistentQueue/EMPTY
			   discovered-map {(:key start) true}
			   u start
         new-graph graph]
		(if (nil? u)
      ;; Base case: all vertices have been explored
			new-graph
      ;; Queue any of the current node's neighbors that haven't been
      ;; discovered
			(let [neighbor-keys
				    (filter #(not (contains? discovered-map %))
					          (:connections u))
				    neighbors
				    (map #(assoc
                   (get-vertex graph %) :parent (:key u))
					       neighbor-keys)
				    new-discovered
				    (as-> discovered ds
					    (into clojure.lang.PersistentQueue/EMPTY
                    (concat ds neighbors)))]
        ;; Further processing of `u` or the `(u, v)` edges can go here
        (println (str "Exploring vertex " (:key u) ", neighbors: "
                      (join ", " neighbor-keys)))        
        ;; Proceed to exploring the next vertice, adding `u`'s
        ;; neighbors to the discovered pile
        (recur
				 (pop new-discovered)
				 (into discovered-map
						   (map #(hash-map % true) neighbor-keys))
				 (peek new-discovered)
         ;; Update each of `u`'s neighbors to show `u` is the parent
         (reduce #(assoc-in %1 [:vertices %2 :parent] (:key u))
                 new-graph
                 neighbor-keys))))))
```

> The `discovered-map` property here is just used to see which vertices have already been discovered, more efficiently than iterating over the `discovered` queue.

[[ more detailed breakdown ]]


## Appendix: Creating a Vertex Explorer with `async.core`
	Notice the `println` in the above code? You can replace that with any processing you need to do on the graph, such as recording results in another data structure or emitting your next Sudoku move.

	But what if your program has multiple uses for the BFS, or you don't want to embed application logic inside of the graph algorithm? In this case, we can harness Clojure's `async.core` system to create a **channel** that emits each vertex as it gets explored.

	This only requires a miniscule change to our function:

```clojure
;; Note: for ClojureScript programs, replace both instances of
;; `clojure.lang.PersistentQueue/EMPTY` below with `#queue []`
(use '[clojure.string :only (join)]
     '[cljs.core.async
       :as async
       :refer [>! <! go chan take]])

(defn bfs-channel
  "Return a 2-tuple where the first element is `graph`, with each
  vertex's `:parent` property updated, and the second element is an
  async.core channel that emits each vertex as it is explored."
	[graph start]
  (let [out (chan)]
	  (loop [discovered clojure.lang.PersistentQueue/EMPTY
			     discovered-map {(:key start) true}
			     u start
           new-graph graph]
		  (if (nil? u)
        ;; Base case: all vertices have been discovered
			  [new-graph
         (take (count (:vertices new-graph)) out)]
        ;; Get all of the current node's neighbors that haven't been
        ;; discovered
			  (let [neighbor-keys
				      (filter #(not (contains? discovered-map %))
					            (:connections u))
				      neighbors
				      (map #(assoc
                     (get-vertex graph %) :parent (:key u))
					         neighbor-keys)
				      new-discovered
				      (as-> discovered ds
					      (into clojure.lang.PersistentQueue/EMPTY
                      (concat ds neighbors)))]
          ;; Emit the vertex being explored to `out`
          (go (>! out u))
          ;; Proceed to exploring the next vertice, adding `u`'s
          ;; neighbors to the discovered pile
          (recur
				   (pop new-discovered)
				   (into discovered-map
						     (map #(hash-map % true) neighbor-keys))
				   (peek new-discovered)
           ;; Update each of `u`'s neighbors to show `u` is the parent
           (reduce #(assoc-in %1 [:vertices %2 :parent] (:key u))
                   new-graph
                   neighbor-keys)))))))
```

	Now we can read from the `out` channel, and do all processing outside of the `bfs` function:
```clojure
	example
```

	*Freakin' sweet!*
___

I hope you've enjoyed reading this article half as much as I've enjoyed playing with these graphs!

If you find any bugs, mastermind any improvements, or want to work with me on a project, [feel free to shoot me a message](https://nathanclonts.com/#contact)!

Good luck, and happy hacking.

	