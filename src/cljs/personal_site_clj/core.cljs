(ns personal-site-clj.core
	(:require [personal-site-clj.animations :as animations]))

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



(defn render [] nil)
