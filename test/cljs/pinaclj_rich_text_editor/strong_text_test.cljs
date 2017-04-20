(ns pinaclj-rich-text-editor.strong-text-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.strong-text :as strong-text]
            [pinaclj-rich-text-editor.zipper :as zipper]
            [clojure.zip :as zip]))

(deftest initialize []
  (is (= false (:strong (strong-text/initialize {})))))

(deftest toggle []
  (testing "toggles if the user presses the required key stroke"
    (is (= true (:strong (strong-text/toggle {:strong false} #{:B :meta})))))
  (testing "does not toggle for other key strokes"
    (is (= false (:strong (strong-text/toggle {:strong false} #{:C :meta})))))
  (testing "toggles off if it's already on"
    (is (= false (:strong (strong-text/toggle {:strong true} #{:B :meta}))))))

(def empty-doc (zipper/->zip [:root]))
(def strong-doc (zip/next (zipper/->zip [:root [:strong {:key 1} "A"]])))
(def strong-em-doc (zip/next (zip/next (zipper/->zip [:root [:strong {:key 1} [:em {:key 2}]]]))))
(def text-doc (zip/next (zipper/->zip [:root [:p {:key 1} "test node"]])))

(defn- perform [state c]
  (-> (:doc-loc (strong-text/handle state c))
      zip/root
      second))

; todo - need to test selection focus has been updated too
(deftest handle []
  (testing "inserts tag if it is not already in one"
    (is (= [:strong {:key 5}] (perform {:strong true
                                        :doc-loc empty-doc
                                        :next-key-fn (fn [] 5)} \X))))
  (testing "does not insert tag if it isn't toggled"
    (is (= nil (perform {:strong false
                         :doc-loc empty-doc} \X))))
  (testing "does not insert tag if it's already in one"
    (is (= [:strong {:key 1} "A"] (perform {:strong true
                                            :doc-loc strong-doc
                                            :next-key-fn (fn [] 5)} \X))))
  (testing "does not insert tag if that tags appears somewhere higher in the tree"
    (is (= [:strong {:key 1} [:em {:key 2}]]
           (perform {:strong true
                     :doc-loc strong-em-doc
                     :next-key-fn (fn [] 5)} \X))))
  (testing "moves out of the tag if it has been turned off but currently in it"
    (is (= [1 1 0]
           (:selection-focus (strong-text/handle {:strong false
                                                  :doc-loc strong-doc
                                                  :selection-focus [1 0 1]} \X)))))
  (testing "splits a text node when inserting tag"
    (let [state (strong-text/handle {:strong true
                                     :doc-loc text-doc
                                     :selection-focus [1 0 4]
                                     :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} "test" [:strong {:key 2}] " node"]] (zip/root (:doc-loc state))))
      (is (= [2 0 0] (:selection-focus state)))))
  (testing "does not return empty text nodes when splitting at lhs"
    (let [state (strong-text/handle {:strong true
                                     :doc-loc text-doc
                                     :selection-focus [1 0 0]
                                     :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2}] "test node"]] (zip/root (:doc-loc state))))))
  (testing "does not return empty text nodes when splitting at rhs"
    (let [state (strong-text/handle {:strong true
                                     :doc-loc text-doc
                                     :selection-focus [1 0 9]
                                     :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} "test node" [:strong {:key 2}]]] (zip/root (:doc-loc state)))))))
