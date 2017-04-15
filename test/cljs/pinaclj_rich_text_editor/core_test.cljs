(ns pinaclj-rich-text-editor.core-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.core :as core]))

(defn- raise-event [event-name c {:keys [shift meta]}]
  (let [event (.createEvent js/document "Event")]
    (set! (.-charCode event) (.charCodeAt c))
    (set! (.-keyCode event) (.charCodeAt c))
    (set! (.-shiftKey event) shift)
    (set! (.-metaKey event) meta)
    (.initEvent event event-name true true)
    (.dispatchEvent js/document event)))

(defn- type-key [c & modifiers]
  (if (raise-event "keydown" c modifiers)
    (raise-event "keypress" c modifiers)))

(defn- perform [initial-doc action]
  (let [editor-node (.createElement js/document "div")]
    (core/load-doc initial-doc)
    (core/attach-editor editor-node)
    (action)
    (core/render editor-node)
    (.-innerHTML editor-node)))

(deftest typing []
  (testing "typing a character inserts a new paragraph with that element"
    (is (= "<p>C</p>" (perform [:p ""] #(type-key \C :shift true)))))
  (testing "typing a control character does not cause a character to appear"
    (is (= "<p></p>" (perform [:p ""] #(type-key \B :meta true)))))
  (testing "typing the bold character and then text causes bold text to appear"
    (is (= "<p><b>C</b></p>" (perform [:p ""] #(do (type-key \B :meta true)
                                                   (type-key \C :shift true)))))))
