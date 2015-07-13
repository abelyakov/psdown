(ns psdown.core
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [clojure.walk :as w])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;div id goods_cont
;

(def url "http://pro-sport-russia.com/shop/t-shirt/dzjudo")
(defn get-content [url] ((client/get url) :body))

(defn get-dom-as-data [url]
  (-> url
      (client/get)
      :body
      hickory/parse
      hickory/as-hickory))

;is current dom element = <div id="goods_cont"> ...
(defn is-goods-cont [x]
  (and (map? x)
       (= (:type x) :element)
       (= "goods_cont" (get-in x [:attrs :id]))))



(defn extract-goods-cont [content]
  (let [result (atom [])]
    (w/postwalk
      #(let [goods-cont (is-goods-cont %)]
        (if goods-cont
          (swap! result conj %)
          %)) content)
    (first @result)))

(defn is-product-url [x]
  (and (map? x)
       (= (:tag x) :a)
       (.contains (get-in x [:attrs :href]) "/desc")))

;for each tree node:
; if (check-fun node) returns true
; add (apply-funct node) to result
(defn extract-all [content check-func apply-func]
  (let [result (atom [])]
    (w/prewalk
      #(let [suitable (check-func %)]
        (when suitable (swap! result conj (apply-func %)))
        %)
      content)
    @result))

;from url to ${product_url1 product_url2 product_url3}
(defn extract-product-urls [url]
  (let [dom-data (get-dom-as-data url)
        goods-cont (extract-goods-cont dom-data)
        product-urls (into #{} (extract-all goods-cont is-product-url #(get-in % [:attrs :href])))]
    product-urls))

(def purl (first (extract-product-urls url)))

(defn get-product-data [url]
  (let [full-url (str "http://www.pro-sport-russia.com" url)]
    (get-dom-as-data full-url)))
