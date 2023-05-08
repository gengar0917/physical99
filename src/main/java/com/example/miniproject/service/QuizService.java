package com.example.miniproject.service;

import com.example.miniproject.dto.*;
import com.example.miniproject.entity.Quiz;
import com.example.miniproject.entity.SolvedQuiz;
import com.example.miniproject.entity.User;
import com.example.miniproject.repository.QuizRepository;
import com.example.miniproject.repository.SolvedQuizRepository;
import com.example.miniproject.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final SolvedQuizRepository solvedQuizRepository;
    private final UserRepository userRepository;

    // 퀴즈 등록
    @Transactional
    public BasicResponseDto<?> register(QuizRequestDto quizRequestDto, User user) {
        Quiz quiz = new Quiz(quizRequestDto, user.getUserId());
        quizRepository.save(quiz);

        return BasicResponseDto.setSuccess("퀴즈 등록 성공!", quiz.getId());
    }

    // 개별 퀴즈 조회
    @Transactional(readOnly = true)
    public BasicResponseDto<SolvingQuizResponseDto> findById(Long id, User user) {
        Quiz quiz = quizRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("해당 퀴즈가 없습니다."));
        List<String> answerList = new ArrayList<>(  );
        answerList.add(quiz.getCorrect());
        if (quiz.getIncorrect1()!=null) {answerList.add(quiz.getIncorrect1());}
        if (quiz.getIncorrect2()!=null) {answerList.add(quiz.getIncorrect2());}
        if (quiz.getIncorrect3()!=null) {answerList.add(quiz.getIncorrect3());}
        if(answerList.size() > 2) {
            Collections.shuffle(answerList);
        }

        SolvedQuiz solvedQuiz = solvedQuizRepository.findByUserIdAndQuizId(user.getId(), id);

        if (solvedQuiz != null) {
            SolvingQuizResponseDto solvingQuizResponseDto = new SolvingQuizResponseDto(quiz, answerList, solvedQuiz.getSolved());
            if (!solvedQuiz.getSolved()) return BasicResponseDto.setSuccess("히히 틀렸음", solvingQuizResponseDto);
            return BasicResponseDto.setSuccess("이미 맞춘 문제입니다.", solvingQuizResponseDto);
        }
        SolvingQuizResponseDto solvingQuizResponseDto = new  SolvingQuizResponseDto(quiz, answerList, false);
        return BasicResponseDto.setSuccess("도전하지 않은 문제입니다", solvingQuizResponseDto);
    }

    // 전체 퀴즈 조회
    @Transactional(readOnly = true)
    public BasicResponseDto<List<QuizResponseDto>> findAll() {
        List<Quiz> quizzes = quizRepository.findAll();
        return BasicResponseDto.setSuccess("전체 퀴즈 조회 성공!",quizzes.stream().map(QuizResponseDto::new).collect(Collectors.toList()));
    }

    // 해결한 문제 조회 -> 마이페이지로 활용하면 어떨까
    @Transactional(readOnly = true)
    public List<SolvedQuiz> SolvedListByUser(Long id) {
        return solvedQuizRepository.selectSolvedQuiz(id);
    }

    // 문제해결
    @Transactional
    public BasicResponseDto<?> solvingQuiz(Long id, AnswerRequestDto answerRequestDto, User user){
        Quiz quiz = quizRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("퀴즈가 존재하지 않습니다.")
        );

        SolvedQuiz existSolvedQuiz = solvedQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId());

        if(existSolvedQuiz != null){
            if(!existSolvedQuiz.getSolved()){
                return BasicResponseDto.setSuccess("이미 문제를 맞추셨습니다!", null);
            }
            return BasicResponseDto.setSuccess("이미 문제를 맞추셨습니다!", null);
        }else {
            if (StringUtils.equals(quiz.getCorrect(), answerRequestDto.getAnswer())) {
                SolvedQuiz solvedQuiz = new SolvedQuiz(user);
                solvedQuiz.setSolved(true);

                quiz.addSolvedQuiz(solvedQuiz);
                solvedQuizRepository.save(solvedQuiz);

                user.setSolvedQuizCnt(solvedQuizRepository.countSolvedQuiz(user.getId()));
                userRepository.save(user);
                return BasicResponseDto.setSuccess("정답입니다~!", null);
            } else{
                SolvedQuiz solvedQuiz = new SolvedQuiz(user);
                solvedQuiz.setSolved(false);

                quiz.addSolvedQuiz(solvedQuiz);
                solvedQuizRepository.save(solvedQuiz);

                user.setSolvedQuizCnt(solvedQuizRepository.countSolvedQuiz(user.getId()));
                userRepository.save(user);
                return BasicResponseDto.setSuccess("틀렸습니다!", null);
            }
        }
    }

    // 퀴즈 게시물 수정
    @Transactional
    public BasicResponseDto<?> update(Long id, AmendRequestDto amendRequestDto, User user) {
        Quiz quiz = quizRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 퀴즈가 없습니다.")
        );

        if(!StringUtils.equals(quiz.getId(), user.getId())) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        } else {
            quiz.update(amendRequestDto);
            return BasicResponseDto.setSuccess("퀴즈를 수정하였습니다.", null);
        }
    }

    // 퀴즈 게시물 삭제
    @Transactional
    public BasicResponseDto<?> deleteAll(Long id, User user) {
        Quiz quiz = quizRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 퀴즈가 없습니다.")
        );
        if(!StringUtils.equals(quiz.getId(), user.getId())) {
            return BasicResponseDto.setFailed("아이디가 같지 않습니다!");
        } else {
            quizRepository.delete(quiz);
            return BasicResponseDto.setSuccess("퀴즈가 삭제되었습니다.", null);
        }
    }
}
