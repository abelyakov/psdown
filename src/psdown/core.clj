(ns psdown.core
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [clojure.walk :as w])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def url "http://pro-sport-russia.com/shop/t-shirt/dzjudo")

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

(defn is-product-url [x]
  (and (map? x)
       (= (:tag x) :a)
       (.contains (get-in x [:attrs :href]) "/desc")))

;for each tree node:
; if (check-fun node) returns true
; add (apply-funct node) to result
(defn extract-all [cont check-func apply-func]
  (let [result (atom [])]
    (w/prewalk
      #(let [suitable (check-func %)]
        (when suitable (swap! result conj (apply-func %)))
        %)
      cont)
    @result))

(defn extract-first [cont check-func apply-func]
  (let [result (atom [])]
    (w/postwalk
      #(let [suitable (check-func %)]
        (if suitable
          (swap! result conj (apply-func %))
          %))
      cont)
    (first @result)))

(defn extract-goods-cont [cont]
  (extract-first cont is-goods-cont identity))


;from url to ${product_url1 product_url2 product_url3}
(defn extract-product-urls [url]
  (let [dom-data (get-dom-as-data url)
        goods-cont (extract-goods-cont dom-data)
        product-urls (into #{} (extract-all goods-cont is-product-url #(get-in % [:attrs :href])))]
    product-urls))



(def purl (-> url extract-product-urls first))

(defn get-product-content [url]
  (let [full-url (str "http://www.pro-sport-russia.com" url)]
    (get-dom-as-data full-url)))

(def purl-content (get-product-content purl))

(defn is-header [x]
  (and
    (map? x)
    (= (:tag x) :h1)))

(defn is-breadcrumb [x]
  (and
    (map? x)
    (= (get-in x [:attrs :itemtype]) "http://data-vocabulary.org/Breadcrumb")))


(defn get-product-images-tumbnails [cont]
  (let [nobr (extract-first cont #(= (:tag %) :nobr) #(:content %))
        images (->> nobr
                    (filter map?)
                    (filter not-empty)
                    (map :attrs)
                    (map #(select-keys % [:alt :src]))
                    first)]
    images))

;; "var el=getElementById('ipreview'); el.src='/_sh/11/1141_1.jpg'; el.setAttribute('idx',1);" -> /_sh/11/1141_1.jpg
(defn extract-image-url [str]
  (let [search-result (re-find (re-pattern "(/_sh.*?)';") str)]
    (second search-result)))

(defn is-main-image [x]
  (let [str (:onclick x)]
    (when (instance? String str) (.contains str "_bldCont1"))))

(defn get-main-image [cont]
  (extract-first cont is-main-image #(-> % :src)))

(defn get-product-images [cont]
  (let [nobr (extract-first cont #(= (:tag %) :nobr) #(:content %))
        images (extract-all nobr #(= "gphoto" (:class %)) #(-> % :onclick extract-image-url))
        main-image (get-main-image cont)]
    (if (some #{main-image} images)
      images
      (conj images main-image))))


(defn get-product-data [url]
  (let [cont (get-product-content url)
        title-block (extract-first cont is-header #(:content %))
        name (first title-block)
        articul (-> title-block second :content second :content first)
        category (last (extract-all cont is-breadcrumb #(-> % :content first :content first :content first)))
        images (get-product-images cont)]
    {:url url :name name :articul articul :category category :images images}))

(defn extract-category-products [url]
  (let [purls (extract-product-urls url)
        pdata (map get-product-data purls)]
    pdata))
;(pprint (map :images res))
