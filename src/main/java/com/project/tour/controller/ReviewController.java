package com.project.tour.controller;



import com.project.tour.domain.*;
import com.project.tour.domain.Package;
import com.project.tour.oauth.dto.SessionUser;
import com.project.tour.oauth.service.LoginUser;
import com.project.tour.service.MemberService;
import com.project.tour.service.PackageService;
import com.project.tour.service.ReviewReplyService;
import com.project.tour.service.ReviewService;
import com.project.tour.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final MemberService memberService;

    private final ReviewReplyService reviewReplyService;

    private final PackageService packageService;




    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/write")
    public String write1(Model model, ReviewForm reviewForm,Principal principal){

        Member member = memberService.getMember(principal.getName());

        //결제완료된 부킹리스트 가져오기
        Long memberId = member.getId();
        int status = 2; // 0:예약확인중 1:결제대기중 2:결제완료

        List<UserBooking> bookingReview =  reviewService.getBookingReview(memberId,status);

        System.out.println(bookingReview.size());

        model.addAttribute("bookingReview",bookingReview);

        return "review/review_write";

    }

    @PreAuthorize("isAuthenticated()") //로그인 해야지만 작성 가능
    @PostMapping(value = "/write")
    public String write2(@Valid ReviewForm reviewForm, BindingResult bindingResult,Principal principal,
            @RequestParam("image") MultipartFile multipartFile, @RequestParam("packageNum") Long packageNum) throws IOException{

        if(bindingResult.hasErrors()){
            return "review/review_write";
        }

        Package reviewPackage = packageService.getPackage(packageNum);

        Member member = memberService.getMember(principal.getName());

        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());

        reviewForm.setReviewImage(fileName);

        Review review = reviewService.create(reviewForm.getSubject(),reviewForm.getContent(),
                reviewForm.getReviewImage(), reviewForm.getScore(),member,reviewPackage);

        String uploadDir =  "review-photo/" + review.getId();

        FileUploadUtil.saveFile(uploadDir,fileName,multipartFile);

        return "redirect:/review/abroadList";

    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/update/{id}")
    public String update1(Model model,ReviewForm reviewForm, @PathVariable("id") Long id, Principal principal){

        Review review = reviewService.getReview(id);

        if(!review.getAuthor().getEmail().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정 권한이 없습니다");
        }

        reviewForm.setSubject(review.getSubject());
        reviewForm.setContent(review.getContent());
        reviewForm.setScore(review.getScore());

        Member member = memberService.getMember(principal.getName());

        //결제완료된 부킹리스트 가져오기
        Long memberId = member.getId();
        int status = 2; // 0:예약확인중 1:결제대기중 2:결제완료

        List<UserBooking> bookingReview =  reviewService.getBookingReview(memberId,status);

        System.out.println(bookingReview.size());

        model.addAttribute("bookingReview",bookingReview);

        model.addAttribute("review", review);

        return "review/review_write";

    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/update/{id}")
    public String update2(@Valid ReviewForm reviewform, BindingResult bindingResult,
                          @PathVariable("id") Long id,@RequestParam("image") MultipartFile multipartFile,Principal principal,
                          @RequestParam("packageNum") Long packageNum)  throws IOException {

        if(bindingResult.hasErrors()){
            return "review/review_write";
        }

        Review review = reviewService.getReview(id);

        if(!review.getAuthor().getEmail().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정 권한이 없습니다");
        }

        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());

        reviewform.setReviewImage(fileName);

        Package reviewPackage = packageService.getPackage(packageNum);

        reviewService.update(review,reviewform.getSubject(),reviewform.getContent(),
                reviewform.getReviewImage(),reviewform.getScore(),reviewPackage);

        String uploadDir =  "review-photo/" + review.getId();

        FileUploadUtil.saveFile(uploadDir,fileName,multipartFile);

        return String.format("redirect:/review/article/%s",id);

    }

    @RequestMapping (value = "/article/{id}")
    public String article(Model model, @PathVariable("id") Long id, ReviewReplyForm reviewReplyForm,
                          Principal principal,@LoginUser SessionUser user){

        Review review = reviewService.getReview(id);



        Member member;

        if(memberService.existByEmail(principal.getName())){

            member = memberService.getName(principal.getName());

        }else{

            member = memberService.getName(user.getEmail());

        }

        Long id2 = member.getId();


        model.addAttribute("replyLike",reviewReplyService.getReplyLike(id2,id));

        model.addAttribute("reviewLike",reviewService.getReviewLike(id2,id));

        model.addAttribute("review",review);

        return "review/review_article";

    }

    @RequestMapping (value = "/abroadList")
    public String abroadList(Model model, @PageableDefault Pageable pageable,Principal principal,@LoginUser SessionUser user ){

        Member member;

        if(memberService.existByEmail(principal.getName())){

            member = memberService.getName(principal.getName());

        }else{

            member = memberService.getName(user.getEmail());

        }


        Page<Review> paging = reviewService.getList(pageable);
        model.addAttribute("paging",paging);

        return "review/review_Abroad_List";

    }

    @RequestMapping (value = "/jejuList")
    public String jejuList(Model model, @PageableDefault Pageable pageable){

        Page<Review> paging = reviewService.getList(pageable);
        model.addAttribute("paging",paging);

        return "review/review_Jeju_List";

    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/delete/{id}")
    public String delete(Principal principal, @PathVariable("id") Long id){

        Review review = reviewService.getReview(id);

        if(!review.getAuthor().getEmail().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"삭제 권한이 없습니다");
        }

        reviewService.delete(review);

        return "redirect:/review/abroadList";

    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String reviewVote(Model model, Principal principal, @PathVariable("id") Long id,@LoginUser SessionUser user){

        Review review = reviewService.getReview(id);

        Member member;

        if(memberService.existByEmail(principal.getName())){

            member = memberService.getName(principal.getName());

        }else{

            member = memberService.getName(user.getEmail());

        }

        if(reviewService.getReviewLike(member.getId(),id)){
            reviewService.deleteLike(member.getId(),id);

        }else{
            reviewService.vote(review,member);
        }






        return String.format("redirect:/review/article/%s",id);

    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reply/{id}")
    public  String writeReply(Model model, @PathVariable("id") Long id, @Valid ReviewReplyForm reviewReplyForm,
                              BindingResult bindingResult, Principal principal){

        Review review = reviewService.getReview(id);
        Member member = memberService.getMember(principal.getName());

        if(bindingResult.hasErrors()){
            model.addAttribute("review",review);
            return "review/review_article"; //실패하면 리뷰디테일 띄우기

        }

        Review_reply review_reply = reviewReplyService.create(review,reviewReplyForm.getContent(),member);

        return String.format("redirect:/review/article/%s#reply_%s",
                review_reply.getReviewNum().getId(),review_reply.getId());

    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/replyUpdate/{id}")
    public String updateReply(ReviewReplyForm reviewReplyForm, @PathVariable("id") Long id, Model model,
                              BindingResult bindingResult,Principal principal){

        Review_reply review_reply = reviewReplyService.getReply(id);

        if(!review_reply.getAuthor().getEmail().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정 권한이 없습니다");
        }

        reviewReplyForm.setContent(review_reply.getContent());

        return "review/review_reply_write";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/replyUpdate/{id}")
    public String modifyReply(@Valid ReviewReplyForm reviewReplyForm, BindingResult bindingResult,
                               @PathVariable("id") Long id, Principal principal){

        if(bindingResult.hasErrors()){
            return "review/review_reply_write";
        }

        Review_reply review_reply = reviewReplyService.getReply(id);

        if(!review_reply.getAuthor().getEmail().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정 권한이 없습니다");
        }

        reviewReplyService.update(review_reply,reviewReplyForm.getContent());

        return String.format("redirect:/review/article/%s#reply_%s",
                         review_reply.getReviewNum().getId(),review_reply.getId());

    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/replyDelete/{id}")
    public String deleteReply(@PathVariable("id")Long id,Principal principal){
        Review_reply review_reply = reviewReplyService.getReply(id);

        if(!review_reply.getAuthor().getEmail().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"삭제 권한이 없습니다");
        }

        reviewReplyService.delete(review_reply);

        return String.format("redirect:/review/article/%s",review_reply.getReviewNum().getId());

    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/replyVote/{id}")
    public String reviewReplyVote(Principal principal, @PathVariable("id") Long id,@LoginUser SessionUser user){


        Review_reply review_reply = reviewReplyService.getReply(id);

        Review review = reviewService.getReview(review_reply.getReviewNum().getId());

        Member member;

        if(memberService.existByEmail(principal.getName())){

            member = memberService.getName(principal.getName());

        }else{

            member = memberService.getName(user.getEmail());

        }

        member = memberService.getMember(principal.getName());

        Long id2 = member.getId();

        if(reviewReplyService.getReplyLike(id2, review.getId())){

            reviewReplyService.deleteReplyLike(id2, review.getId());


        }else{
            reviewReplyService.vote(review_reply,member,review);

        }



        return String.format("redirect:/review/article/%s#reply_%s",
                review_reply.getReviewNum().getId(),review_reply.getId());

    }

}
