(ns personal-site-clj.animations
	(:require [quil.core :as q :include-macros true]
						[quil.middleware :as m]
						[personal-site-clj.avl-tree :as avl]
            [personal-site-clj.cities-graph :as cities]))

; @TODO import json data and draw it. Check out:
; https://github.com/JulianBirch/cljs-ajax

;	;	;	;	;	;	;	;	;	;	;	;	;
; Animations & drawings
;	;	;	;	;	;	;	;	;	;	;	;	;
;

(defn mostly-inside-window? [element-id]
	"Check if over 50% of element is within the viewport (vertically)."
	(let [el-rect (. (.getElementById js/document element-id)
                   getBoundingClientRect)
				window-height (.-innerHeight js/window)]
		(and
			(< (.-top el-rect) (/ window-height 2))
			(> (.-bottom el-rect) (/ window-height 2)))))

(def FRAME-RATE 30)
(def UPDATE-TREE-PERIOD (* 2 FRAME-RATE))
(def PROGRESS-INCREMENT (/ 0.5 FRAME-RATE))
(def tree-settings
	{:x-spread 200
	 :node-r 20
	 :level-height 100})

; flag to determine if things need to be redrawn in this frame
(def dirty-state (atom true))

; stores last :x :y positions of nodes, to help smoothly animate to new positions
; format:
;		{ <node-key-0> {:x 42, :y 42},
;			<node-key-1> {:x 21  :y 84},
;			... }
(def old-positions (atom {}))


(defn queue-insertion [{tree :tree :as state}]
	(-> state
		(assoc :tree (avl/avl-insert (inc (avl/tree-max tree)) nil tree))
		(assoc :frames-to-next-update UPDATE-TREE-PERIOD)))

(defn setup []
	(do
		(q/frame-rate FRAME-RATE)
		(q/background (q/color 0 0 0 1))
		(q/color-mode :hsb 360 100 100)
		(q/stroke 177 99 99)
		(q/stroke-weight 6)
		(let [tree (avl/map->avl
                (into {} (map vector (range 0 1) (repeat 1 nil))))]
			; initial state
			{:x 400
			 :y 300
			 :tree tree
			 :rotation-angle 0
			 :progress-to-next-step 0
			 :last-key 0
			 :frames-to-next-update UPDATE-TREE-PERIOD
			 :max-node-count 15})))

(defn mouse-moved [state event]
	"Save angle between base of tree and mouse."
	(let [dy (- (:y state) (:y event))
				dx (- (:x state) (:x event))]
		(swap! dirty-state (constantly true))
		(assoc state :rotation-angle
           (+ (Math/atan2 dy dx) (/ Math/PI 2)))))

(defn mouse-clicked [state event]
	"Increase number of nodes displayed."
	(-> state
		(assoc :max-node-count (inc (:max-node-count state)))))

(defn update-tree [{tree :tree, frames :frames-to-next-update,
										max-nodes :max-node-count :as state}]
	(let [update-time? (<= frames 0)
				at-max-count? (>= (avl/count-nodes tree) max-nodes)]
		(if (mostly-inside-window? "avl-canvas")
			(if (and update-time? (not at-max-count?))
				(do
					(swap! dirty-state (constantly true))
					(-> state
						(assoc :progress-to-next-step 0)
						(queue-insertion)))
				(-> state
					(assoc :frames-to-next-update (- frames 1))
					(assoc
						:progress-to-next-step
						(+ (:progress-to-next-step state) PROGRESS-INCREMENT))))

			; when outside window, stop drawing
			(do
				(swap! dirty-state (constantly false))
				state))))

(defn draw-tree
	([tree x y progress-to-next-step]
		(draw-tree tree x y nil nil 0 progress-to-next-step))
	([tree x y last-x last-y depth progress-to-next-step]
		(let [next-y (+ y (:level-height tree-settings))
					r (:node-r tree-settings)
					x-delta (/
                   (:x-spread tree-settings)
                   (Math/pow 2 (inc depth)))
					last-position (get @old-positions (:key tree))]

			; old-x/y is the last "stable" location the node ended
			(let [old-x (get last-position :x)
						old-y (get last-position :y)
						p progress-to-next-step]
        ; current-x/y interpolates between the "new" stable x/y and
        ; the original spot
				(let [current-x
              (if (nil? old-x) x (+ (* p x) (* (- 1 p) old-x)))
							current-y
              (if (nil? old-y) y (+ (* p y) (* (- 1 p) old-y)))]
					; draw line from parent
					(if (not (or (nil? last-x) (nil? last-y)))
						(q/line
             last-x (+ last-y (/ r 2))
             current-x (- current-y (/ r 2))))
					; draw the ellipse
					(q/fill 177 99 99 0)
					(q/ellipse current-x current-y r r)
					(q/fill 360)
					(q/text (:key tree) (- current-x 5) (+ current-y 5))
					; and continue down the tree....
					(if (nil? (:left tree))
						nil
						(draw-tree
							(:left tree)
							(- current-x x-delta)
							next-y
							current-x
							current-y
							(inc depth)
							p))
					(if (nil? (:right tree))
						nil
						(draw-tree
							(:right tree)
							(+ current-x x-delta)
							next-y
							current-x
							current-y
							(inc depth)
							p))
					(if (> progress-to-next-step (- 1 PROGRESS-INCREMENT))
						(swap! old-positions assoc (:key tree) {:x x :y y})))))))


(defn avl-draw [state]
	; traverse & draw the tree
	(when @dirty-state
		(q/background (q/color 0 0 0 1))
		(q/translate (:x state) (:y state))
		(q/rotate (:rotation-angle state))
		(draw-tree (:tree state) 0 0 (:progress-to-next-step state))))

(def CANVAS-WIDTHS (- (.-innerWidth js/window) 25))

(q/defsketch fun-mode-times-avl
	:host "avl-canvas"
	:size [CANVAS-WIDTHS 750]
	:setup setup
	:draw avl-draw
	:update update-tree
	:mouse-moved mouse-moved
	:mouse-clicked mouse-clicked
	:middleware [m/fun-mode])

