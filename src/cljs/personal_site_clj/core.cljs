(ns personal-site-clj.core
	(:require [quil.core :as q :include-macros true]
						[quil.middleware :as m]
						[personal-site-clj.avl-tree :as avl]))

(enable-console-print!)

; Allow iteration over HTML elements
(extend-type js/HTMLCollection
	ISeqable
	(-seq [array] (array-seq array 0)))

; Intro / above fold stuff
(def intro-text "Hi, I'm Nathan")
(defn type-name [text]
	(let [el (.getElementById js/document "typing-intro")]
		(set! (.-innerHTML el) (str (.-innerHTML el) (subs text 0 1)))
		(js/setTimeout (fn [] (type-name (subs text 1))) (+ 50 (rand-int 50)))))
(.addEventListener
	js/window
	"DOMContentLoaded"
	(fn []
		(set! (.-innerHTML (.getElementById js/document "typing-intro")) "")
		(type-name intro-text)))

;	;	;	;	;	;	;	;	;	;	;	;	;
; Event handlers
;	;	;	;	;	;	;	;	;	;	;	;	;
(defn trigger-when-class-clicked [class-name f]
	(let [elements (.getElementsByClassName js/document class-name)]
		(doall (map #(.addEventListener % "click" f) elements))))

(defn toggle-menu [event]
	(let [hamburger (js/document.getElementById "hamburger")
				menu (js/document.getElementById "menu")]
		(.toggle (.-classList menu) "hidden")
		(.toggle (.-classList hamburger) "is-active")))
(defn close-menu [event]
	(let [hamburger (js/document.getElementById "hamburger")
				menu (js/document.getElementById "menu")]
		(.add (.-classList menu) "hidden")
		(.remove (.-classList hamburger) "is-active")))

(trigger-when-class-clicked "hamburger" toggle-menu)
(trigger-when-class-clicked "menu-link" close-menu)


;	;	;	;	;	;	;	;	;	;	;	;	;
; Animations & drawings
;	;	;	;	;	;	;	;	;	;	;	;	;
;
(def FRAME-RATE 30)
(def UPDATE-TREE-FREQUENCY (* 2 FRAME-RATE))
(def PROGRESS-INCREMENT (/ 0.5 FRAME-RATE))
(def tree-settings
	{:x-spread 200
	 :node-r 20
	 :level-height 100})

; flag to determine if things need to be redrawn in this frame
(def dirty-state (atom true))
; stores last :x :y positions of nodes, to help smoothly animate to new positions
(def old-positions (atom {}))


(defn queue-insertion [{tree :tree :as state}]
	(-> state
		(assoc :tree (avl/avl-insert (inc (avl/tree-max tree)) nil tree))
		(assoc :progress-to-next-step 0)))

(defn setup []
	(do
		(q/frame-rate FRAME-RATE)
		(q/background (q/color 0 0 0 1))
		(q/color-mode :hsb 360 100 100)
		(q/stroke 177 99 99)
		(q/stroke-weight 6)
		(let [tree (avl/map->avl (into {} (map vector (range 0 1) (repeat 1 nil))))]
			; initial state
			{:x 400
			 :y 300
			 :tree tree
			 :rotation-angle 0
			 :progress-to-next-step 0
			 :last-key 0})))

(defn mouse-moved [state event]
	"Save angle between base of tree and mouse."
	(let [dy (- (:y state) (:y event))
				dx (- (:x state) (:x event))]
		(swap! dirty-state (constantly true))
		(assoc state :rotation-angle (+ (Math/atan2 dy dx) (/ Math/PI 2)))))

(defn mouse-clicked [state event]
	(queue-insertion state))

(defn update-tree [{tree :tree :as state}]
	(let [update-time? (= 0 (mod (q/frame-count) UPDATE-TREE-FREQUENCY))
				; default to stop adding nodes when at a height of 5
				do-more-updates? (< (avl/tree-height tree) 5)]
		(if (and do-more-updates? update-time?)
			(do
				(swap! dirty-state (constantly true))
				(queue-insertion state))
			(assoc state
				:progress-to-next-step
				(+ (:progress-to-next-step state) PROGRESS-INCREMENT)))))

(defn draw-tree
	([tree x y progress-to-next-step]
		(draw-tree tree x y nil nil 0 progress-to-next-step))
	([tree x y last-x last-y depth progress-to-next-step]
		(let [next-y (+ y (:level-height tree-settings))
					r (:node-r tree-settings)
					x-delta (/ (:x-spread tree-settings) (Math/pow 2 (inc depth)))
					last-position (get @old-positions (:key tree))]

			(let [old-x (get last-position :x)
						old-y (get last-position :y)
						p progress-to-next-step]
				(let [current-x (if (nil? old-x) x (+ (* p x) (* (- 1 p) old-x)))
							current-y (if (nil? old-y) y (+ (* p y) (* (- 1 p) old-y)))]
					; draw line from parent
					(if (not (or (nil? last-x) (nil? last-y)))
						(q/line last-x (+ last-y (/ r 2)) current-x (- current-y (/ r 2))))
					; draw the ellipse
					(q/fill 177 99 99 0)
					(q/ellipse current-x current-y r r)
					(q/fill 360)
					(q/text (:key tree) (- current-x 5) (+ current-y 5))
					; and continue down the tree
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

(defn draw-buttons []
	(q/fill 177 99 99 0)
	(q/line 50 50 50 100)
	(q/line 25 75 75 75))

(defn draw [state]
	; traverse & draw the tree
	(when @dirty-state
		(println "re-drawing")
		(q/background (q/color 0 0 0 1))
		(draw-buttons)
		(q/translate (:x state) (:y state))
		(q/rotate (:rotation-angle state))
		(draw-tree (:tree state) 0 0 (:progress-to-next-step state))))
		

(q/defsketch fun-mode-times
	:host "test-canvas"
	:size [(- (.-innerWidth js/window) 25) 750]
	:setup setup
	:draw draw
	:update update-tree
	:mouse-moved mouse-moved
	:mouse-clicked mouse-clicked
	:middleware [m/fun-mode])

(defn render [] nil)
