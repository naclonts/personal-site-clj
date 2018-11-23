
# Breadth First Search Algorithm in Clojure (Payloads Included!)

Below is an implementation of a graph structure and breadth-first search (BFS) algorithm that allows each vertex or node to contain any payload you like -- a map, a string, whatever.


1. [Why a graph algorithm?](#why-a-dang-graph-algorithm)
2. [The Graph and the Vertex](#the-graph-and-the-vertex)
3. [Fundamental Operations](#fundamental-operations)
4. [The Search they call Breadth First](#the-search-they-call-breadth-first)

## Why a dang graph algorithm?
After starting a new job working with Python and JavaScript, I felt the itch to learn something in the functional tradition. Eventually I settled on Clojure, a dialect of the venerable Lisp. Working with algorithms and data structures is one of the best ways to learn a new language, so I translated a breadth-first search algorithm into Clojure, and in the process found some interesting differences between the Lispey code and the classic imperative style. 

There are other implementations of BFS for Clojure on the web, but some of them use graph representations that don't have the ability to assign arbitrary payloads to vertices. Sometimes a simple integer for each vertex will suffice, but giving yourself the option to tie any data to a graph opens up a ton of interesting applications.

Much credit is due to the free book [Problem Solving with Algorithms and Data Structures using Python](http://interactivepython.org/runestone/static/pythonds/index.html), which provided the inspiration and basis for this implementation. I recommend it to anyone who'd like to beef up their algorithmic chops.

## The Graph and the Vertex
First off, let's define our data structures. Our vertices are simple key value objects, and the graph is a collection of vertices.
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
	 ;; Notice the thread "->" operation here, which inserts `vertices` as the
	 ;; first argument of the two `assoc` calls
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

### The Search they call Breadth First
Our breadth first search function will accept a graph object and a starting vertex. It will return the graph with each vertex's `:parent` property updated to the appropriate parent's key for a BFS path. Here's the function:

```clojure
(defn bfs
	"Return `graph` with each vertex's `:parent` property updated."
	[graph start]
	(loop [discovered (create-queue start)
			   discovered-map {(:key start) true}
			   u start
         new-graph graph]
		(if (nil? u)
      ;; Base case: all vertices have been discovered
			new-graph
      ;; Get all of the current node's neighbors that haven't been
      ;; discovered
			(let [neighbor-keys
				    (filter #(not (contains? discovered-map %))
					          (:connections u))
				    neighbors
				    (map #(assoc (get-vertex graph %) :parent (:key u))
					       neighbor-keys)
				    new-discovered
				    (as-> discovered ds
					    ;; NOTE: cljs vs. clj
					    (into clojure.lang.PersistentQueue/EMPTY
                    (concat ds neighbors)))]
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



[[ more detailed breakdown ]]


___

I hope you've enjoyed reading this article half as much as I've enjoyed playing with these graphs.

Good luck, and happy hacking.