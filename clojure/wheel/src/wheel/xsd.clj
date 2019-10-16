(ns wheel.xsd
  (:import [javax.xml.validation SchemaFactory]
           [javax.xml XMLConstants]
           [org.xml.sax SAXException]
           [java.io StringReader File]
           [javax.xml.transform.stream StreamSource]))

(defn validate [^File xsd-file ^String xml-content]
  (let [validator (-> (SchemaFactory/newInstance
                       XMLConstants/W3C_XML_SCHEMA_NS_URI)
                      (.newSchema xsd-file)
                      (.newValidator))]
    (try
      (->> (StringReader. xml-content)
           StreamSource.
           (.validate validator))
      nil
      (catch SAXException e (.getMessage e)))))

(comment
  (let [xsd-file (clojure.java.io/as-file
                  (clojure.java.io/resource
                   "oms/message_schema/ranging.xsd"))
        sample   "
<EXTNChannelList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UA\" EAN=\"UA_EAN_1\" 
                     ItemID=\"SKU1\" RangeFlag=\"Y\"/>
    <EXTNChannelItem ChannelID= \"UA\" EAN= \"UA_EAN_2 \"
ItemID= \"SKU2\" RangeFlag= \"Y\"/>      
  </EXTNChannelItemList>
  <EXTNChannelItemList>
    <EXTNChannelItem ChannelID=\"UB\" EAN=\"UB_EAN_3\" 
                     ItemID=\"SKU3\" RangeFlag=\"Y\"/>
  </EXTNChannelItemList>
</EXTNChannelList>
    "]
    (validate xsd-file sample)))