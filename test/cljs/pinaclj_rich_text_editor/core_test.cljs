(ns pinaclj-rich-text-editor.core-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.dom-fixture :as dom-fixture]
            [pinaclj-rich-text-editor.core :as core]
            [pinaclj-rich-text-editor.dom :as dom]))

(test/use-fixtures :each dom-fixture/clear-dom)

(defn- raise-key-event [event-name c {:keys [shift meta]}]
  (let [event (.createEvent js/document "Event")]
    (set! (.-charCode event) (.charCodeAt c))
    (set! (.-keyCode event) (.charCodeAt c))
    (set! (.-shiftKey event) shift)
    (set! (.-metaKey event) meta)
    (.initEvent event event-name true true)
    (.dispatchEvent js/document event)))

(defn- set-range
  ([node-key text-node text-position]
   (let [start-node (dom-fixture/->text-node (dom-fixture/find-dom-node node-key) text-node)]
     (dom-fixture/set-range [start-node text-position] [start-node text-position]))))

(defn- type-key [c & modifiers]
  (if (raise-key-event "keydown" c modifiers)
    (raise-key-event "keypress" c modifiers)))

(defn- remove-all-keys [node]
  (when-not (dom/text-node? node)
    (.removeAttribute node "key")
    (doall (map remove-all-keys (array-seq (.-childNodes node))))))

(defn- perform [initial-children action]
  (let [editor-node (dom-fixture/create-root)]
    (apply core/attach-editor editor-node initial-children)
    (action)
    (remove-all-keys editor-node)
    (.-innerHTML editor-node)))

(deftest loading []
  (testing "it renders nodes"
    (core/attach-editor (dom-fixture/create-root) [:p {:key "1"} "Rendered"])
    (is (not (nil? (dom-fixture/find-dom-node "1")))))
  (testing "renders multiple nodes"
    (let [root-node (dom-fixture/create-root)]
      (core/attach-editor root-node [:p "a"] [:p "b"])
      (remove-all-keys root-node)
      (is (= "<p>a</p><p>b</p>" (.-innerHTML root-node))))))

(deftest typing []
  (testing "typing a character inserts that character into existing paragraph"
    (is (= "<p>C</p>" (perform [[:p ""]] #(type-key \C :shift true)))))
  (testing "typing a control character does not cause a character to appear"
    (is (= "<p></p>" (perform [[:p ""]] #(type-key \B :meta true)))))
  (testing "typing the bold character and then text causes bold text to appear"
    (is (= "<p><strong>C</strong></p>" (perform [[:p ""]] #(do (type-key \B :meta true)
                                                   (type-key \C :shift true))))))
  (comment (testing "opens a paragraph element if there isn't one already"
    (is (= "<p>C</p>" (perform [] #(type-key \C :shift true)))))))

(deftest positioning []
  (testing "text is inserted at last-clicked position"
    (is (= "<p>Hello, world</p>"
           (perform [[:p {:key "initial"} "Hello world"]]
                    #(do (set-range "initial" 0 5)
                         (type-key \,))))))
  (comment (testing "positions when text node is the third child"
    (is (= "<p><strong>Hello</strong> world!</p>"
           (perform [[:p {:key "1"} [:strong {:key "2"} "Hello"] " world"]]
                    #(do (set-range "1" 1 6)
                         (type-key \!))))))))

