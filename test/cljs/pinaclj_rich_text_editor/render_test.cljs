(ns pinaclj-rich-text-editor.render-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.render :as render]))

(defn- render [node]
  (let [elem (.createElement js/document "div")]
    (render/render node elem)
    (.-innerHTML elem)))

(deftest renders []
  (testing "an empty paragraph"
    (is (= "<p></p>" (render [:p]))))
  (testing "a character in a paragraph"
    (is (= "<p>C</p>" (render [:p "C"]))))
  (testing "a tree of nodes"
    (is (= "<p><b>C</b></p>" (render [:p [:b "C"]])))))
