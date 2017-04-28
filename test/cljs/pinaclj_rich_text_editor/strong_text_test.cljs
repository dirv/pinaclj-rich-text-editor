(ns pinaclj-rich-text-editor.strong-text-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [pinaclj-rich-text-editor.selection :as selection]
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

(def empty-doc [:root])
(def strong-doc [:root [:p {:key 0} [:strong {:key 1} "A"]]])
(def strong-em-doc [:root [:p {:key 0} [:strong {:key 1} [:em {:key 2} ""]]]])
(def strong-em-text-doc [:root [:p {:key 0} [:strong {:key 1} [:em {:key 2} "test node"]]]])
(def text-doc [:root [:p {:key 1} "test node"]])
(def strong-text-doc [:root [:p {:key 1} [:strong {:key 2} "test node"]]])

(defn- focus [state]
  (let [root-loc (zipper/->zip (:doc-loc state))]
    (assoc state
         :doc-loc
         (or (selection/move-loc-to-focus root-loc (:selection-focus state))
             root-loc))))

(defn- perform [state c]
  (-> (focus state)
      (strong-text/handle c)))

(defn- doc [state]
  (-> state
      (:doc-loc)
      zip/root))

; todo - need to test selection focus has been updated too
(deftest handle []
  (testing "inserts tag if it is not already in one"
    (let [state {:strong true
                 :doc-loc empty-doc
                 :selection-focus [0 0 0]
                 :next-key-fn (fn [] 5)}]
     (is (= [:strong {:key 5}] (zip/node (:doc-loc (perform state \X)))))))

  (testing "does not insert tag if it isn't toggled"
    (is (= empty-doc
           (doc (perform {:strong false
                          :doc-loc empty-doc
                          :select-focus [0 0 0]} \X)))))

  (testing "does not insert tag if it's already in one"
    (is (= strong-doc
           (doc (perform {:strong true
                          :doc-loc strong-doc
                          :selection-focus [1 0 1]
                          :next-key-fn (fn [] 5)} \X)))))

  (testing "does not insert tag if that tags appears somewhere higher in the tree"
    (is (= strong-em-doc
           (doc (perform {:strong true
                          :doc-loc strong-em-doc
                          :selection-focus [2 0 0]
                          :next-key-fn (fn [] 5)} \X)))))

  (testing "moves out of the tag if it has been turned off but currently in it"
    (let [state {:strong false
                 :doc-loc strong-doc
                 :selection-focus [1 0 1]
                 :next-key-fn (fn [] 5)}]
      (is (= [0 1 0]
           (:selection-focus (perform state \X))))))

  (testing "splits a text node when inserting tag"
    (let [state (perform {:strong true
                          :doc-loc text-doc
                          :selection-focus [1 0 4]
                          :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} "test" [:strong {:key 2}] " node"]] (doc state)))
      (is (= [2 0 0] (:selection-focus state)))))

  (testing "does not return empty text nodes when splitting at lhs"
    (let [state (perform {:strong true
                          :doc-loc text-doc
                          :selection-focus [1 0 0]
                          :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2}] "test node"]] (doc state)))))

  (testing "does not return empty text nodes when splitting at rhs"
    (let [state (perform {:strong true
                          :doc-loc text-doc
                          :selection-focus [1 0 9]
                          :next-key-fn (fn [] 2)} \X)]
      (is (= [:root [:p {:key 1} "test node" [:strong {:key 2}]]] (doc state)))))

  (testing "splitting a node when leaving a tag"
    (let [state (perform {:strong false
                          :doc-loc strong-text-doc
                          :selection-focus [2 0 4]
                          :next-key-fn (fn [] 3)} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2} "test"] "" [:strong {:key 3} " node"]]] (doc state)))
      (is (= [1 1 0] (:selection-focus state)))))

  (testing "creates empty nodes when leaving a strong tag at lhs"
    (let [state (perform {:strong false
                          :doc-loc strong-text-doc
                          :selection-focus [2 0 0]} \X)]
      (is (= [:root [:p {:key 1} "" [:strong {:key 2} "test node"]]] (doc state)))
      (is (= [1 0 0] (:selection-focus state)))))

  (testing "creates empty nodes when leaving a strong tag at rhs"
    (let [state (perform {:strong false
                          :doc-loc strong-text-doc
                          :selection-focus [2 0 9]} \X)]
      (is (= [:root [:p {:key 1} [:strong {:key 2} "test node"] ""]] (doc state)))
      (is (= [1 1 0] (:selection-focus state)))))

  (testing "recreates in-between nodes when closing this one"
    (let [state (perform {:strong false
                          :doc-loc strong-em-doc
                          :selection-focus [2 0 0]} \X)]
      (is (= [:root [:p {:key 0} [:em {:key 2} ""]]] (doc state)))
      (is (= [2 0 0] (:selection-focus state)))))

  (testing "it splits nodes correctly when in the middle of chain"
    (let [state (perform {:strong false
                          :doc-loc strong-em-text-doc
                          :selection-focus [2 0 4]
                          :next-key-fn (let [next-key (atom 3)]
                                         (fn []
                                           (let [new-key @next-key]
                                             (swap! next-key inc)
                                             new-key)))} \X)]
      (is (= [:root [:p {:key 0} [:strong {:key 1} [:em {:key 2} "test"]] [:em {:key 3} ""] [:strong {:key 4} [:em {:key 5} " node"]]]] (doc state))))))
