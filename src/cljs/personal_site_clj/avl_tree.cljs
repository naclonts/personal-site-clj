(ns personal-site-clj.avl-tree
  (:require [clojure.string :refer [join]]))

; Binary (AVL) tree implementation
; thanks to https://two-wrongs.com/purely-functional-avl-trees-in-common-lisp.html
(defrecord AvlTree [key value left right])

(defn tree-max [tree]
	(if (nil? (:right tree))
		(:key tree)
		(recur (:right tree))))

(defn tree-height
	([tree] (tree-height tree 0))
	([tree height] 
		(if (nil? tree)
			height
			(max (tree-height (:left tree) (inc height))
					 (tree-height (:right tree) (inc height))))))

(defn balance-factor [node]
	"Get the balance factor of subtree rooted at node."
	(case (- (tree-height (:right node))
					 (tree-height (:left node)))
		-2 :imbalanced-left
		-1 :left-heavy
		 0 :balanced
		+1 :right-heavy
		+2 :imbalanced-right))

(declare avl-node)
(defn rotate-left [node]
	"Return tree rotated left."
	(let [{:keys [key value left right]} node
				height (tree-height node)]
		(avl-node
			(:key right)
			(:value right)
			(avl-node key value left (:left right))
			(:right right))))

(defn rotate-right [node]
	"Return tree rotated right."
	(let [{:keys [key value left right]} node
				height (tree-height node)]
		(avl-node
			(:key left)
			(:value left)
			(:left left)
			(avl-node key value (:right left) right))))

(defn avl-node [key value left right]
	(let [node (->AvlTree key value left right)]
		(case (balance-factor node)
			(:left-heavy :balanced :right-heavy)
				node

			:imbalanced-left
				(case (balance-factor left)
					:left-heavy
						(rotate-right node)
					:right-heavy
						(avl-node key value (rotate-left left) right))

			:imbalanced-right
				(case (balance-factor right)
					:left-heavy
						(avl-node key value left (rotate-right right))
					:right-heavy
						(rotate-left node)))))

(defn avl-insert [key value tree]
	(if (nil? tree)
		(avl-node key value nil nil)
		(avl-node
			(:key tree)
			(:value tree)
			(if (< key (:key tree))
				(avl-insert key value (:left tree))
				(:left tree))
			(if (< key (:key tree))
				(:right tree)
				(avl-insert key value (:right tree))))))

(defn lookup [key {:keys [node-key value left right] :as tree}]
	"Returns all values associated with key in tree."
	(cond
		(< key node-key) (lookup key left)
		(< node-key key) (lookup key right)
		:else (cons value
						(concat (lookup key left) (lookup key right)))))

(defn tree-tabs [n]
  (join (repeat n "\t")))

(defn tree-visualize
  ([tree] (tree-visualize tree 0))
	([{:keys [key value left right] :as tree} depth]
    (if tree
      (str (tree-visualize right (inc depth)) (tree-tabs depth) key "\n" (tree-visualize left (inc depth)))
      (str (tree-tabs depth) "~\n"))))

(defn tree-print [tree] (println (tree-visualize tree)))

(defn map->avl [key-val-map]
	(reduce
		(fn [tree [key val]] (avl-insert key val tree))
		nil
		(seq key-val-map)))

(defn count-nodes 
	([tree] (count-nodes tree 0))
	([tree n]
		(if (nil? tree)
			0
			(+
				1
				(count-nodes (:right tree))
				(count-nodes (:left tree))))))
