(ns pinaclj-rich-text-editor.render-test
  (:require-macros [cljs.test :refer [deftest testing is async]])
  (:require [cljs.test :as test]
            [incremental-dom :as idom]
            [pinaclj-rich-text-editor.render :as render]))

(defn- render [node]
  (let [elem (.createElement js/document "div")]
    (render/render-all [node] elem)
    (.-innerHTML elem)))

(deftest renders []
  (testing "an empty paragraph"
    (is (= "<p></p>" (render [:p]))))
  (testing "a character in a paragraph"
    (is (= "<p>C</p>" (render [:p "C"]))))
  (testing "a tree of nodes"
    (is (= "<p><b>C</b></p>" (render [:p [:b "C"]]))))
  (testing "a parent element with attribute"
    (is (= "<p class=\"test\"></p>" (render [:p {:class "test"} ""]))))
  (testing "mutliple children"
    (is (= "<p><b>C</b><i>D</i></p>" (render [:p [:b "C"] [:i "D"]])))))

(deftest idom-methods []
  (testing "uses idom/elementVoid for leaf elements"
    (let [calls (atom [])]
      (with-redefs [idom/elementVoid (fn [& args] (swap! calls conj args))]
        (render [:p])
        (is (= 1 (count @calls)))
        (is (= ["p" nil []] (first @calls))))))
  (testing "uses idom/text for text nodes"
    (let [calls (atom [])]
      (with-redefs [idom/text (fn [& args] (swap! calls conj args))]
        (render "text")
        (is (= 1 (count @calls)))
        (is (= ["text"] (first @calls))))))
  (testing "uses idom/elementVoid for parents with attrs"
    (let [calls (atom [])]
      (with-redefs [idom/elementVoid (fn [& args] (swap! calls conj args))]
        (render [:p {:test "1"}])
        (is (= 1 (count @calls)))
        (is (= ["p" nil [] "test" "1"]) (first @calls))))))

(deftest element-keys []
  (testing "passes key to parent elements"
    (let [given-key (atom nil)]
      (with-redefs [idom/elementOpen #(reset! given-key %2)
                idom/elementClose (fn [& _] )]
        (render [:p {:key "A"} [:b]])
        (is (= "A" @given-key)))))
  (testing "passes key for leaf elements"
    (let [given-key (atom nil)]
      (with-redefs [idom/elementVoid #(reset! given-key %2)]
        (render [:p {:key "A"}])
        (is (= "A" @given-key))))))
