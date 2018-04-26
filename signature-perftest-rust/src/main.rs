#![feature(duration_extras)]
#![feature(plugin, decl_macro)]
#![plugin(rocket_codegen)]

extern crate rocket;

extern crate openssl;

use std::env;
use std::thread;
use std::io::Read;
use std::fs::File;
use std::time::Instant;
use std::time::Duration;

use openssl::sign::Verifier;
use openssl::hash::MessageDigest;
use openssl::pkcs12::Pkcs12;

#[get("/is_alive")]
fn is_alive() -> &'static str {
    "I'm alive, I swear!"
}

fn read_file(path: &str) -> Vec<u8> {
    let mut pkcs12_bytes: Vec<u8> = vec![];
    let mut file = File::open(path).unwrap();
    file.read_to_end(&mut pkcs12_bytes);
    pkcs12_bytes
}

fn to_millis(duration: &Duration) -> u64 {
    duration.subsec_millis() as u64 + (duration.as_secs() * 1000)
}

fn main() {
    let mut args = env::args();
    args.next().unwrap();
    let handle = thread::spawn(move || {
        rocket::ignite().mount("/", routes![is_alive]).launch();
    });

    let loading_start = Instant::now();
    let pkcs12 = Pkcs12::from_der(read_file(&args.next().unwrap()).as_ref()).unwrap();
    let keypair = pkcs12.parse(&args.next().unwrap().as_str()).unwrap().pkey;

    let signed_bytes = read_file(&args.next().unwrap());
    let signature_bytes = read_file(&args.next().unwrap());
    println!("Application started in {} ms", to_millis(&loading_start.elapsed()));

    let validation_start = Instant::now();
    for i in 0..100 {
        let mut verifier = Verifier::new(MessageDigest::sha1(), &keypair).unwrap();
        verifier.update(signed_bytes.as_ref());
        if !verifier.verify(signature_bytes.as_ref()).unwrap() {
            panic!("Unable to verify signature");
        }
    }

    println!("Run finished in: {} ms", to_millis(&validation_start.elapsed()));


    handle.join().unwrap();
}

