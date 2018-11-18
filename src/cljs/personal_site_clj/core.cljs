(ns personal-site-clj.core
	(:require [personal-site-clj.animations :as animations]
						[ajax.core :refer [POST]]))

(enable-console-print!)

; Allow iteration over HTML elements
(extend-type js/HTMLCollection
	ISeqable
	(-seq [array] (array-seq array 0)))

; Intro / above fold stuff
  (def intro-text "Build Your Bridges")
(defn type-name [text]
	(let [el (.getElementById js/document "typing-intro")
				new-char (subs text 0 1)]
		(set! (.-innerHTML el) (str (.-innerHTML el) new-char))
		(js/setTimeout
			(fn [] (type-name (subs text 1)))
			(if (= new-char " ") 0 (+ 50 (rand-int 50))))))
(.addEventListener
	js/window
	"DOMContentLoaded"
	(fn []
		(set! (.-innerHTML (.getElementById js/document "typing-intro")) "")
		(type-name intro-text)))

;	;	;	;	;	;	;	;	;	;	;	;	;
; Event handlers
;	;	;	;	;	;	;	;	;	;	;	;	;

; Menus
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


; Contact form - handle via Ajax to prevent page reloads.
(let [form-el (.getElementById js/document "contact-form")
			button (.getElementById js/document "contact-form-button")]
	(defn update-info-message! [msg]
		(set! (.-innerHTML (.getElementById js/document "contact-response-message")) msg))
	(defn handle-response [res]
		(update-info-message! res))
	(defn ^:export submit-form [event]
		(.preventDefault event)
		(update-info-message! "Sending your message...")
		(POST "/contact-form"
			{:body (js/FormData. form-el)
			 :handler handle-response
			 :error-handler handle-response})
		(.reset form-el))
	(.addEventListener form-el "submit" submit-form))


(defn render [] nil)
