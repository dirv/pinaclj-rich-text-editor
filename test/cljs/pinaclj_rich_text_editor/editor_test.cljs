(ns pinaclj-rich-text-editor.editor-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test]
            [pinaclj-rich-text-editor.editor :as editor]
            [pinaclj-rich-text-editor.hiccup :as hiccup]
            [pinaclj-rich-text-editor.selection :as selection]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(def empty-doc (zipper/->zip [:div {}]))
(def doc (zipper/->zip [:div [:p {}]]))
(def hello-doc (-> (zipper/->zip [:div [:p {} "Hello world"]]) zip/down))
(def multi-doc (zipper/find-loc (zipper/->zip [:div
                              [:p {:key "1"}]
                              [:p {:key "2"}]
                              [:p {:key "3"}]])
                                (partial hiccup/matches-attr? :key "3")))

(defn- insert [doc-loc position c]
  (-> {:doc-loc doc-loc :selection-focus position}
                (editor/insert-into-loc c)
                :doc-loc
                zip/node))

(deftest insert-into-loc []
  (testing "creates a new paragraph if none exists"
    (is (= [:p {} "a"] (insert empty-doc selection/unknown-position \a))))
  (testing "inserts at specified point"
    (is (= [:p {} "Hello, world"] (insert hello-doc [nil 0 5] \,))))
  (testing "inserts with the right key"
    (is (= [:p {:key "3"} "x"] (insert multi-doc ["3" 0 0] \x)))))
