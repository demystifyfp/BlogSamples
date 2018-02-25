#r "packages/Hopac/lib/net45/Hopac.Core.dll"
#r "packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"

open Hopac

let helloWorldJob = job {
  printfn "Hello, World!"
}

run helloWorldJob

let longerHelloWorldJob = job {
  do! timeOutMillis 2000
  printfn "Hello, World!"
}

#time "on"
run longerHelloWorldJob
#time "off"

let createJob jobId delayInMillis  = job {
  printfn "starting job:%d" jobId
  do! timeOutMillis delayInMillis
  printfn "completed job:%d" jobId
}

let jobs = [
  createJob 1 4000
  createJob 2 3000
  createJob 3 2000
]

let concurrentJobs = Job.conIgnore jobs


#time "on"
run concurrentJobs
#time "off"


type Product = { 
  Id : int
  Name : string
}

let getProduct id = job {
  do! timeOutMillis 2000
  return {Id = id; Name = "My Awesome Product"}
}

type Review = {
  ProductId : int
  Author : string
  Comment : string
}

let getProductReviews id = job {
  do! timeOutMillis 3000
  return [
    {ProductId = id; Author = "John"; Comment = "It's awesome!"}
    {ProductId = id; Author = "Sam"; Comment = "Great product"}
  ]
}

type ProductWithReviews = {
  Id : int
  Name : string
  Reviews : (string * string) list
}

let getProductWithReviews id = job {
  let! product = getProduct id
  let! reviews = getProductReviews id
  return {
    Id = id
    Name = product.Name
    Reviews = reviews |> List.map (fun r -> r.Author,r.Comment)
  }
}


#time "on"
getProductWithReviews 2 |> run
#time "off"


open Hopac.Infixes

let getProductWithReviews2 id = job {
  let! product, reviews = getProduct id <*> getProductReviews id
  return {
    Id = id
    Name = product.Name
    Reviews = reviews |> List.map (fun r -> r.Author,r.Comment)
  }
}

#time "on"
getProductWithReviews2 1 |> run
#time "off"