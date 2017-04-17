(ns pinaclj-rich-text-editor.editor-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test]
            [pinaclj-rich-text-editor.editor :as editor]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def empty-doc (zipper/->zip [:div {}]))
(def doc (zipper/->zip [:div [:p {}]]))
(def hello-doc (zipper/->zip [:div [:p {} "Hello world"]]))
(def multi-doc (zipper/->zip [:div
                              [:p {:key "1"}]
                              [:p {:key "2"}]
                              [:p {:key "3"}]]))

(def insert (comp zip/node editor/insert-into-loc))
(deftest insert-into-loc []
  (testing "it inserts at the first paragraph if no node key is set"
    (is (= [:p {} "a"] (insert doc [nil 0 0] \a))))
  (testing "creates a new paragraph if none exists"
    (is (= [:p {} "a"] (insert empty-doc [nil 0 0] \a))))
  (testing "inserts at specified point"
    (is (= [:p {} "Hello, world"] (insert hello-doc [nil 0 5] \,))))
  (testing "inserts with the right key"
    (is (= [:p {:key "3"} "x"] (insert multi-doc ["3" 0 0] \x)))))
