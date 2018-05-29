#r "./packages/Hopac/lib/net45/Hopac.Core.dll"
#r "./packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "./packages/Hopac/lib/net45/Hopac.dll"

open Hopac

type Image = Image of string
type ImageJob = Image -> Job<Image>

let imageJob delay jobName (Image image) = job {
  printfn "%s Started: %s" jobName image
  do! timeOutMillis delay
  let newImage = sprintf "%s [%s]" image jobName
  printfn "%s Completed: %s" jobName newImage
  return (Image newImage)
}

let scaleImage = imageJob 2000 "Scaling"
let filterImage = imageJob 1500 "Filtering"
let displayImage = imageJob 500 "Displaying"

type BoundedWorker (queueLength, f : ImageJob) =
  let inMb = new BoundedMb<Image>(queueLength)
  member __.CreateJob (count : int, outMb : BoundedMb<Image>) =
    BoundedMb.take inMb
    |> Alt.afterJob f
    |> Alt.afterJob (BoundedMb.put outMb)
    |> Job.forN count

  member __.CreateJob (count : int) =
    BoundedMb.take inMb
    |> Alt.afterJob f
    |> Alt.afterFun (fun _ -> ())
    |> Job.forN count
  member __.InMb = inMb


// Ch<'a> -> 'a list -> Alt<unit>
let rec loadImages inMb inputs =
  match inputs with
  | [] -> Alt.always ()
  | x :: xs ->
    BoundedMb.put inMb x
    |> Alt.afterJob (fun _ -> loadImages inMb xs)

let pipeline images = 
  let imagesCount = List.length images
  let queueLength = 3
  let imageScaler = BoundedWorker(queueLength, scaleImage)
  let imageFilterer = BoundedWorker(queueLength, filterImage)
  let imageDisplayer = BoundedWorker(queueLength, displayImage)
  
  loadImages imageScaler.InMb images |> start
  [ imageScaler.CreateJob(imagesCount, imageFilterer.InMb)
    imageFilterer.CreateJob(imagesCount,  imageDisplayer.InMb)
    imageDisplayer.CreateJob(imagesCount)]
  |> Job.conIgnore
  


let images = [Image "Foo.png"; Image "Bar.png";Image "Baz.png"] 

#time "on"
pipeline images |> run
#time "off"

